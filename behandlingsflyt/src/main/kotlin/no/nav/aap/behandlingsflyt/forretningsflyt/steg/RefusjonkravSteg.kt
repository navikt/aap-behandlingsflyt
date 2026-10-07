package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.SYSTEMBRUKER
import no.nav.aap.behandlingsflyt.arena.ArenaMigreringMapper
import no.nav.aap.behandlingsflyt.arena.ArenaMigreringService
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.erSystembruker
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.refusjonkrav.RefusjonkravRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.refusjonkrav.RefusjonkravVurdering
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.MigrerVurderingFraArena
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingService
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

class RefusjonkravSteg(
    private val refusjonkravRepository: RefusjonkravRepository,
    private val tidligereVurderinger: TidligereVurderinger,
    private val avklaringsbehovService: AvklaringsbehovService,
    private val behandlingRepository: BehandlingRepository,
    private val behandlingService: BehandlingService,
    private val unleashGateway: UnleashGateway,
    private val arenaMigreringService: ArenaMigreringService,
) : BehandlingSteg, MigrerVurderingFraArena {
    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        refusjonkravRepository = repositoryProvider.provide(),
        tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider, gatewayProvider),
        avklaringsbehovService = AvklaringsbehovService(repositoryProvider, gatewayProvider),
        behandlingRepository = repositoryProvider.provide(),
        behandlingService = BehandlingService(repositoryProvider, gatewayProvider),
        unleashGateway = gatewayProvider.provide(),
        arenaMigreringService = ArenaMigreringService(repositoryProvider, gatewayProvider),
    )

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        if (kontekst.erMigreringFraArena() && unleashGateway.isEnabled(BehandlingsflytFeature.MigrerRefusjonskravFraArenaAutomatisk)) {
            val erRelevantForMigrering = !tidligereVurderinger.girAvslagEllerIngenBehandlingsgrunnlag(kontekst, type())
            if (erRelevantForMigrering) {
                migrerVurderingFraArena(kontekst)
            }
        }

        val grunnlag = lazy { refusjonkravRepository.hentHvisEksisterer(kontekst.behandlingId) }
        val behandling = behandlingRepository.hent(kontekst.behandlingId)
        val behandlingstype = behandlingService.utledFaktiskBehandlingstype(behandling)

        avklaringsbehovService.oppdaterAvklaringsbehov(
            definisjon = Definisjon.REFUSJON_KRAV,
            vedtakBehøverVurdering = {
                when {
                    else -> when (behandlingstype) {
                        TypeBehandling.Førstegangsbehandling -> {
                            when {
                                tidligereVurderinger.girAvslagEllerIngenBehandlingsgrunnlag(kontekst, type()) -> false
                                erVurdertAutomatisk(grunnlag.value) -> false
                                kontekst.vurderingsbehovRelevanteForSteg.isNotEmpty() -> true
                                else -> {
                                    kontekst.forrigeBehandlingId?.let {
                                        refusjonkravRepository.hentHvisEksisterer(it).isNullOrEmpty()
                                    } ?: true
                                }
                            }
                        }

                        TypeBehandling.Revurdering -> {
                            when {
                                !unleashGateway.isEnabled(BehandlingsflytFeature.KanVurdereRefusjonIRevurdering) -> false
                                tidligereVurderinger.girAvslagEllerIngenBehandlingsgrunnlag(kontekst, type()) -> false
                                kontekst.vurderingsbehovRelevanteForSteg.isNotEmpty() -> true
                                erVurdertAutomatisk(grunnlag.value) -> false
                                else -> false
                            }
                        }

                        else -> false
                    }
                }
            },
            erTilstrekkeligVurdert = {
                grunnlag.value != null
            },
            tilbakestillGrunnlag = {
                kontekst.forrigeBehandlingId
                    ?.let { grunnlag.value }
                    ?.let {
                        refusjonkravRepository.lagre(kontekst.sakId, kontekst.behandlingId, it)
                    }
            },
            kontekst = kontekst
        )

        return Fullført
    }

    private fun erVurdertAutomatisk(grunnlag: List<RefusjonkravVurdering>?): Boolean {
        return grunnlag?.all { it.vurdertAv.erSystembruker()  } ?: false
    }

    override fun migrerVurderingFraArena(kontekst: FlytKontekstMedPerioder) {
        require(kontekst.erMigreringFraArena()) {
            "Kan ikke migrere vurdering fra Arena for sak ${kontekst.sakId} fordi vurderingstype ikke er migrering"
        }

        val refusjonskravFraArena = arenaMigreringService.hentRefusjonskrav(kontekst.sakId)
        arenaMigreringService.lagreMigreringsdataForSporing(kontekst.behandlingId, type(), refusjonskravFraArena)

        val vurdering = ArenaMigreringMapper.mapRefusjonskravVurdering(refusjonskravFraArena)
        refusjonkravRepository.lagre(
            kontekst.sakId, kontekst.behandlingId, listOf(vurdering)
        )
    }

    companion object : FlytSteg {
        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): BehandlingSteg {
            return RefusjonkravSteg(repositoryProvider, gatewayProvider)
        }

        override fun type(): StegType {
            return StegType.REFUSJON_KRAV
        }
    }
}
