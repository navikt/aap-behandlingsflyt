package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.arena.ArenaMigreringMapper
import no.nav.aap.behandlingsflyt.arena.ArenaMigreringService
import no.nav.aap.behandlingsflyt.arena.erOrdinærAap
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovMetadataUtleder
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.PeriodisertVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangarbeid.OvergangArbeidRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.MigrerVurderingFraArena
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.orEmpty
import no.nav.aap.lookup.repository.RepositoryProvider

class VurderSykdomSteg(
    private val sykdomRepository: SykdomRepository,
    private val overgangArbeidRepository: OvergangArbeidRepository,
    private val tidligereVurderinger: TidligereVurderinger,
    private val avklaringsbehovService: AvklaringsbehovService,
    private val arenaMigreringService: ArenaMigreringService,
    private val unleashGateway: UnleashGateway
) : BehandlingSteg, AvklaringsbehovMetadataUtleder, MigrerVurderingFraArena {
    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        sykdomRepository = repositoryProvider.provide(),
        overgangArbeidRepository = repositoryProvider.provide(),
        tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider, gatewayProvider),
        avklaringsbehovService = AvklaringsbehovService(repositoryProvider, gatewayProvider),
        arenaMigreringService = ArenaMigreringService(repositoryProvider, gatewayProvider),
        unleashGateway = gatewayProvider.provide()
    )

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        if (kontekst.erMigreringFraArena() && unleashGateway.isEnabled(BehandlingsflytFeature.MigererSykdomFraArenaAutomatisk)) {
            val harRelevantePerioderForMigrering = nårVurderingErRelevant(kontekst).any { it }
            if (harRelevantePerioderForMigrering) {
                migrerVurderingFraArena(kontekst)
            }
        }

        avklaringsbehovService.oppdaterAvklaringsbehovForPeriodisertYtelsesvilkår(
            definisjon = Definisjon.AVKLAR_SYKDOM,
            tvingerAvklaringsbehov = tvingerAvklaringsbehov(kontekst),
            nårVurderingErRelevant = ::nårVurderingErRelevant,
            nårVurderingErGyldig = { tilstrekkeligVurdert(kontekst) },
            kontekst = kontekst,
            tilbakestillGrunnlag = {
                val vedtatteSykdomsvurderinger = kontekst.forrigeBehandlingId
                    ?.let { sykdomRepository.hentHvisEksisterer(it) }
                    ?.sykdomsvurderinger
                    ?: emptyList()
                sykdomRepository.lagre(kontekst.behandlingId, vedtatteSykdomsvurderinger)
            },
            gjeldendeVurderinger = { hentGjeldendeVurderinger(kontekst) }
        )
        return Fullført
    }

    private fun hentGjeldendeVurderinger(kontekst: FlytKontekstMedPerioder): Tidslinje<PeriodisertVurdering> {
        return sykdomRepository.hentHvisEksisterer(kontekst.behandlingId)?.somSykdomsvurderingstidslinje()
            ?.mapValue { it as PeriodisertVurdering }.orEmpty()
    }

    private fun tvingerAvklaringsbehov(kontekst: FlytKontekstMedPerioder): Set<Vurderingsbehov> {
        val forrigeOvergangArbeidGrunnlag = kontekst.forrigeBehandlingId?.let {
            overgangArbeidRepository.hentHvisEksisterer(it)
        }

        val vedtatteSykdomsvurderinger = kontekst.forrigeBehandlingId?.let {
            sykdomRepository.hentHvisEksisterer(it)?.sykdomsvurderinger ?: emptyList()
        }

        val skalTriggesVedRevurderingOvergangArbeid = forrigeOvergangArbeidGrunnlag?.vurderinger.isNullOrEmpty()
        val skalTriggesVedRevurderingStudent =
            vedtatteSykdomsvurderinger?.none { it.potensieltOppfyltStudent() } == true

        val irrelevanteVurderingsbehov = buildSet {
            if (!skalTriggesVedRevurderingStudent) add(Vurderingsbehov.REVURDER_STUDENT)
            if (!skalTriggesVedRevurderingOvergangArbeid) add(Vurderingsbehov.OVERGANG_ARBEID)
        }
        return kontekst.vurderingsbehovRelevanteForSteg - irrelevanteVurderingsbehov

    }

    override fun nårVurderingErRelevant(kontekst: FlytKontekstMedPerioder): Tidslinje<Boolean> {
        val tidligereVurderingsutfall = tidligereVurderinger.behandlingsutfall(
            kontekst,
            type()
        )

        return tidligereVurderingsutfall.mapValue { behandlingsutfall ->
            when (behandlingsutfall) {
                TidligereVurderinger.IkkeBehandlingsgrunnlag -> false
                is TidligereVurderinger.UunngåeligAvslag -> false
                is TidligereVurderinger.PotensieltOppfylt -> {
                    behandlingsutfall.rettighetstype == null
                }
            }
        }
    }

    override fun migrerVurderingFraArena(kontekst: FlytKontekstMedPerioder) {
        require(kontekst.erMigreringFraArena()) {
            "Kan ikke migrere vurdering fra Arena for sak ${kontekst.sakId} fordi vurderingstype ikke er migrering"
        }

        val sykdomsvurderingFraArena =
            arenaMigreringService.hentSykdomsvurdering(kontekst.sakId)

        /**
         * Kun ordinær AAP støttes for migreringsgruppe 1. Dette vil utvides når andre saker skal migreres
         * på senere tidspunkt.
         */
        require(sykdomsvurderingFraArena.erOrdinærAap()) {
            "Kan ikke migrere sykdomsvurdering fra Arena for sak ${kontekst.sakId} fordi ikke alle vilkår for ordinær AAP er oppfylt"
        }

        /**
         * Lagrer sporing av migreringsdata for sykdomsvurdering fra Arena. Dette er nyttig for å kunne spore
         * hva som var utgangspunktet for vurderingen som opprettes i Kelvin.
         */
        arenaMigreringService.lagreMigreringsdataForSporing(kontekst.behandlingId, stegType, sykdomsvurderingFraArena)

        val vurdering = ArenaMigreringMapper.mapOppfyltOrdinærSykdomsvurdering(
            fraArena = sykdomsvurderingFraArena,
            behandlingId = kontekst.behandlingId,
            vurderingenGjelderFra = kontekst.rettighetsperiode.fom,
        )

        require(vurdering.erOppfyltOrdinærMedUtlededeFelter()) {
            "Kan ikke migrere sykdomsvurdering fra Arena for sak ${kontekst.sakId} fordi vurderingen ikke er oppfylt for ordinær AAP"
        }

        sykdomRepository.lagre(kontekst.behandlingId, listOf(vurdering))
    }

    private fun tilstrekkeligVurdert(kontekst: FlytKontekstMedPerioder): Tidslinje<Boolean> {
        val sykdomGrunnlag = sykdomRepository.hentHvisEksisterer(kontekst.behandlingId)

        return sykdomGrunnlag?.somSykdomsvurderingstidslinje().orEmpty()
            .mapValue { it.vurderingenGjelderFra <= kontekst.rettighetsperiode.tom }
    }

    override val stegType = type()

    companion object : FlytSteg {
        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): BehandlingSteg {
            return VurderSykdomSteg(repositoryProvider, gatewayProvider)
        }

        override fun type(): StegType {
            return StegType.AVKLAR_SYKDOM
        }
    }
}
