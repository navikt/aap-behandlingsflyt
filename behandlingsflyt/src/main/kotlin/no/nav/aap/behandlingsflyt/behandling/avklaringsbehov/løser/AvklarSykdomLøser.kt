package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarSykdomEnkelLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarSykdomLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.YrkesskadeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Sykdomsvurdering
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.utils.Validation
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.lookup.repository.RepositoryProvider
import org.slf4j.LoggerFactory
import kotlin.collections.List

class AvklarSykdomEnkelLøser(
    val repositoryProvider: RepositoryProvider
) : AvklaringsbehovsLøser<AvklarSykdomEnkelLøsning> {
    override fun løs(
        kontekst: AvklaringsbehovKontekst,
        løsning: AvklarSykdomEnkelLøsning
    ): LøsningsResultat {
        return AvklarSykdomLøser(repositoryProvider).løs(kontekst, AvklarSykdomLøsning(løsning.sykdomsvurderinger))
    }

    override fun forBehov(): Definisjon {
        return Definisjon.AVKLAR_SYKDOM
    }
}

class AvklarSykdomLøser(
    private val behandlingRepository: BehandlingRepository,
    private val sykdomRepository: SykdomRepository,
    private val yrkersskadeRepository: YrkesskadeRepository,
) : AvklaringsbehovsLøser<AvklarSykdomLøsning> {

    constructor(repositoryProvider: RepositoryProvider) : this(
        behandlingRepository = repositoryProvider.provide(),
        sykdomRepository = repositoryProvider.provide(),
        yrkersskadeRepository = repositoryProvider.provide(),
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

        return when (val validertLøsning = løsning.valider()) {
            is Validation.Invalid -> throw UgyldigForespørselException(validertLøsning.errorMessage)
            is Validation.Valid -> {
                sykdomRepository.lagre(
                    behandlingId = behandling.id,
                    sykdomsvurderinger = gjeldendeVurderinger,
                )
                LøsningsResultat(
                    begrunnelse = "Vurdering av § 11-5"
                )
            }
        }
    }

    private fun AvklarSykdomLøsning.valider(): Validation<AvklarSykdomLøsning> {
        if (løsningerForPerioder.isEmpty()) {
            return Validation.Invalid(this, "Må sende inn minst én sykdomsvurdering")
        }
        if (løsningerForPerioder.map { it.fom }.distinct().size != løsningerForPerioder.size) {
            return Validation.Invalid(this, "Vurderingene må ha unik 'vurderingenGjelderFra'-dato")
        }
        // TODO: Bør ha validering på konsistente verdier
        return Validation.Valid(this)
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
        sykdomLøsning.segmenter().forEach {
            if (!it.verdi.erKonsistentForSykdom(harYrkesskade, behandling.typeBehandling())) {
                log.info(
                    "Sykdomsvurderingen er ikke konsistent med yrkesskade sykdomsvurdering=[{}] harYrkesskade=[{}]",
                    it.verdi,
                    harYrkesskade,
                )
                throw UgyldigForespørselException("Sykdomsvurdering og yrkesskade har ikke konsistente verdier")
            }
        }
    }

    override fun forBehov(): Definisjon {
        return Definisjon.AVKLAR_SYKDOM
    }
}
