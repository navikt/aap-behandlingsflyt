package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.SYSTEMBRUKER
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
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
import java.time.LocalDateTime

class RefusjonkravSteg(
    private val refusjonkravRepository: RefusjonkravRepository,
    private val tidligereVurderinger: TidligereVurderinger,
    private val avklaringsbehovService: AvklaringsbehovService,
    private val behandlingRepository: BehandlingRepository,
    private val behandlingService: BehandlingService,
    private val unleashGateway: UnleashGateway
) : BehandlingSteg, MigrerVurderingFraArena {
    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        refusjonkravRepository = repositoryProvider.provide(),
        tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider, gatewayProvider),
        avklaringsbehovService = AvklaringsbehovService(repositoryProvider, gatewayProvider),
        behandlingRepository = repositoryProvider.provide(),
        behandlingService = BehandlingService(repositoryProvider, gatewayProvider),
        unleashGateway = gatewayProvider.provide()
    )

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val migreresAutomatiskFraArena = kontekst.erMigreringFraArena()
                && unleashGateway.isEnabled(BehandlingsflytFeature.MigererSykdomFraArenaAutomatisk)
        if (migreresAutomatiskFraArena) {
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
                    // Migrering skjer aldri i en revurdering, så vurderingen skal aldri kunne trigge
                    // et manuelt avklaringsbehov når den er automatisk satt av migreringen.
                    migreresAutomatiskFraArena -> false

                    else -> when (behandlingstype) {
                        TypeBehandling.Førstegangsbehandling -> {
                            when {
                                tidligereVurderinger.girAvslagEllerIngenBehandlingsgrunnlag(kontekst, type()) -> false
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

    override fun migrerVurderingFraArena(kontekst: FlytKontekstMedPerioder) {
        refusjonkravRepository.lagre(
            kontekst.sakId, kontekst.behandlingId, listOf(
                RefusjonkravVurdering(
                    harKrav = false,
                    navKontor = null,
                    vurdertAv = SYSTEMBRUKER,
                    opprettetTid = LocalDateTime.now()
                )
            )
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
