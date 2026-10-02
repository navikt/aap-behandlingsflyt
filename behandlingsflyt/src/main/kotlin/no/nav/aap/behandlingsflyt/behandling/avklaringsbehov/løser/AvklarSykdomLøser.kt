package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarSykdomLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.YrkesskadeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Sykdomsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomsvurderingValideringsfeil
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.lookup.repository.RepositoryProvider
import org.slf4j.LoggerFactory

class AvklarSykdomLøser(
    private val behandlingRepository: BehandlingRepository,
    private val sykdomRepository: SykdomRepository,
    private val yrkersskadeRepository: YrkesskadeRepository,
    private val unleashGateway: UnleashGateway,
) : AvklaringsbehovsLøser<AvklarSykdomLøsning> {

    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        behandlingRepository = repositoryProvider.provide(),
        sykdomRepository = repositoryProvider.provide(),
        yrkersskadeRepository = repositoryProvider.provide(),
        unleashGateway = gatewayProvider.provide(),
    )

    private val log = LoggerFactory.getLogger(javaClass)

    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: AvklarSykdomLøsning): LøsningsResultat {
        val behandling = behandlingRepository.hent(kontekst.kontekst.behandlingId)

        val nyeSykdomsvurderinger = løsning.løsningerForPerioder
            .map { it.toSykdomsvurdering(kontekst.bruker, kontekst.behandlingId()) }

        val eksisterendeSykdomsvurderinger = behandling.forrigeBehandlingId
            ?.let { sykdomRepository.hentHvisEksisterer(it) }
            ?.sykdomsvurderinger
            .orEmpty()

        val gjeldendeVurderinger = eksisterendeSykdomsvurderinger + nyeSykdomsvurderinger

        validerSykdomOgYrkesskadeKonsistens(
            behandling,
            gjeldendeVurderinger
        )

        sykdomRepository.lagre(
            behandlingId = behandling.id,
            sykdomsvurderinger = gjeldendeVurderinger,
        )
        return LøsningsResultat(
            begrunnelse = "Vurdering av § 11-5"
        )
    }

    private fun validerSykdomOgYrkesskadeKonsistens(
        behandling: Behandling,
        gjeldendeSykdomsvurderinger: List<Sykdomsvurdering>,
    ) {
        val sykdomLøsning = SykdomGrunnlag(
            sykdomsvurderinger = gjeldendeSykdomsvurderinger,
            yrkesskadevurdering = null
        ).somSykdomsvurderingstidslinje()
        val yrkesskadeGrunnlag = yrkersskadeRepository.hentHvisEksisterer(behandling.id)

        val harYrkesskade = yrkesskadeGrunnlag?.yrkesskader?.harYrkesskade() == true
        sykdomLøsning.segmenter().forEach { segment ->
            val feil = segment.verdi.validerKonsistensForSykdom(harYrkesskade)
            if (feil.isNotEmpty()) {
                logWarning(harYrkesskade, behandling, segment)

                val meldinger = feil.map { feiltype ->
                    when (feiltype) {
                        SykdomsvurderingValideringsfeil.MANGLER_NEDSATT_ARBEIDSEVNE ->
                            "Du må svare på om arbeidsevnen er nedsatt."

                        SykdomsvurderingValideringsfeil.NEDSATT_ARBEIDSEVNE_STEMMER_IKKE_MED_50_PROSENT ->
                            "Svarene om nedsatt arbeidsevne og 50-prosentgrensen stemmer ikke overens."

                        SykdomsvurderingValideringsfeil.NEDSATT_ARBEIDSEVNE_STEMMER_IKKE_MED_VESENTLIGHET ->
                            "Svarene om nedsatt arbeidsevne og om sykdommen er en vesentlig del stemmer ikke overens."

                        SykdomsvurderingValideringsfeil.MANGLER_VURDERING_AV_YRKESSKADEGRENSE ->
                            "Du må svare på om arbeidsevnen er nedsatt mer enn yrkesskadegrensen."
                    }
                }

                throw UgyldigForespørselException(meldinger.joinToString(" "))
            }
        }
    }

    private fun logWarning(
        harYrkesskade: Boolean,
        behandling: Behandling,
        segment: Segment<Sykdomsvurdering>
    ) {
        log.warn(
            "Sykdomsvurderingen er ikke konsistent med yrkesskade. " +
                    "harYrkesskade: $harYrkesskade, " +
                    "typeBehandling: ${behandling.typeBehandling()}, " +
                    "sykdomsvurdering: ${
                        segment.verdi.copy(
                            begrunnelse = "",
                            yrkesskadeBegrunnelse = "",
                            diagnose = null,
                        )
                    }"
        )
    }

    override fun forBehov(): Definisjon {
        return Definisjon.AVKLAR_SYKDOM
    }
}
