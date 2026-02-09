package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarOvergangUføreEnkelLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.BistandRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangufore.OvergangUføreGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangufore.OvergangUføreRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangufore.OvergangUføreValidering.Companion.nårVurderingErKonsistentMedSykdomOgBistand
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangufore.OvergangUføreVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.behandlingsflyt.utils.toHumanReadable
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.orEmpty
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.LocalDate
import kotlin.collections.orEmpty

class AvklarOvergangUføreLøser(
    private val overgangUforeRepository: OvergangUføreRepository,
    private val sakRepository: SakRepository,
    private val sykdomRepository: SykdomRepository,
    private val bistandRepository: BistandRepository,
    private val unleashGateway: UnleashGateway
) : AvklaringsbehovsLøser<AvklarOvergangUføreEnkelLøsning> {

    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        overgangUforeRepository = repositoryProvider.provide(),
        sakRepository = repositoryProvider.provide(),
        sykdomRepository = repositoryProvider.provide(),
        bistandRepository = repositoryProvider.provide(),
        unleashGateway = gatewayProvider.provide()
    )

    override fun løs(
        kontekst: AvklaringsbehovKontekst,
        løsning: AvklarOvergangUføreEnkelLøsning
    ): LøsningsResultat {
        val løsninger = løsning.løsningerForPerioder ?: listOf(requireNotNull(løsning.overgangUføreVurdering))

        val (behandlingId, sakId, forrigeBehandlingId) = kontekst.kontekst.let {
            Triple(
                it.behandlingId,
                it.sakId,
                it.forrigeBehandlingId
            )
        }

        val rettighetsperiode = sakRepository.hent(sakId).rettighetsperiode

        val vedtatteVurderinger = forrigeBehandlingId
            ?.let { overgangUforeRepository.hentHvisEksisterer(it) }
            ?.vurderinger
            .orEmpty()

        val nyeVurderinger = løsninger.map {
            it.tilOvergangUføreVurdering(
                kontekst.bruker,
                rettighetsperiode.fom,
                behandlingId
            )
        }
        
        if (unleashGateway.isEnabled(BehandlingsflytFeature.ValiderOvergangUfore)) {
            val nyTidslinje = OvergangUføreGrunnlag(
                vurderinger = nyeVurderinger + vedtatteVurderinger
            ).somOvergangUforevurderingstidslinje()
            valider(behandlingId, rettighetsperiode.fom, nyTidslinje)
        }
        
        overgangUforeRepository.lagre(
            behandlingId = behandlingId,
            overgangUføreVurderinger = nyeVurderinger + vedtatteVurderinger
        )

        return LøsningsResultat(
            begrunnelse = nyeVurderinger.joinToString("\n") { it.begrunnelse }
        )
    }

    override fun forBehov(): Definisjon {
        return Definisjon.AVKLAR_OVERGANG_UFORE
    }

    private fun valider(
        behandlingId: BehandlingId,
        kravdato: LocalDate,
        nyTidslinje: Tidslinje<OvergangUføreVurdering>
    ) {
        val sykdomTidslinje = sykdomRepository.hentHvisEksisterer(behandlingId)
            ?.somSykdomsvurderingstidslinje()
            .orEmpty()
        val bistandTidslinje = bistandRepository.hentHvisEksisterer(behandlingId)
            ?.somBistandsvurderingstidslinje()
            .orEmpty()
        val inkonsistentePerioder = nårVurderingErKonsistentMedSykdomOgBistand(
            overgangUføreTidslinje = nyTidslinje,
            sykdomstidslinje = sykdomTidslinje,
            bistandstidslinje = bistandTidslinje,
            kravdato = kravdato
        ).filter { !it.verdi }.perioder().toSet()
        if (inkonsistentePerioder.isNotEmpty()) {
            throw UgyldigForespørselException(
                "Vurderingene for ${inkonsistentePerioder.toHumanReadable()} stemmer ikke med periodene i § 11-6 Sykdom og bistand."
            )
        }
    }
}
