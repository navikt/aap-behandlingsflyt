package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.ResultatUtleder
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.refusjonkrav.RefusjonkravRepository
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

class RefusjonkravSteg private constructor(
    private val refusjonkravRepository: RefusjonkravRepository,
    private val tidligereVurderinger: TidligereVurderinger,
    private val avklaringsbehovService: AvklaringsbehovService,
    private val behandlingRepository: BehandlingRepository,
    private val resultatUtleder: ResultatUtleder,
    private val unleashGateway: UnleashGateway
) : BehandlingSteg {
    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        refusjonkravRepository = repositoryProvider.provide(),
        tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider, gatewayProvider),
        avklaringsbehovService = AvklaringsbehovService(repositoryProvider),
        behandlingRepository = repositoryProvider.provide(),
        resultatUtleder = ResultatUtleder(repositoryProvider),
        unleashGateway = gatewayProvider.provide()
    )

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val grunnlag = lazy { refusjonkravRepository.hentHvisEksisterer(kontekst.behandlingId) }

        avklaringsbehovService.oppdaterAvklaringsbehov(
            definisjon = Definisjon.REFUSJON_KRAV,
            vedtakBehøverVurdering = {
                when (kontekst.vurderingType) {
                    VurderingType.FØRSTEGANGSBEHANDLING -> {
                        when {
                            kontekst.behandlingType == TypeBehandling.Førstegangsbehandling -> !tidligereVurderinger.girAvslagEllerIngenBehandlingsgrunnlag(
                                kontekst,
                                type()
                            )

                            else -> false
                        }
                    }

                    VurderingType.REVURDERING -> {
                        when {
                            unleashGateway.isEnabled(BehandlingsflytFeature.RefusjonkravIRevurdering) && skalVurdereRefusjonkravIRevurdering(
                                kontekst
                            ) -> true

                            else -> false
                        }
                    }

                    VurderingType.UTVID_VEDTAKSLENGDE,
                    VurderingType.MIGRER_RETTIGHETSPERIODE,
                    VurderingType.MELDEKORT,
                    VurderingType.AUTOMATISK_BREV,
                    VurderingType.EFFEKTUER_AKTIVITETSPLIKT,
                    VurderingType.EFFEKTUER_AKTIVITETSPLIKT_11_9,
                    VurderingType.IKKE_RELEVANT -> false
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

    private fun skalVurdereRefusjonkravIRevurdering(kontekst: FlytKontekstMedPerioder): Boolean {
        return !tidligereVurderinger.girAvslagEllerIngenBehandlingsgrunnlag(kontekst, type()) &&
                forrigeBehandlingVarRentAvslag(kontekst)
    }

    private fun forrigeBehandlingVarRentAvslag(kontekst: FlytKontekstMedPerioder): Boolean {
        val forrigeBehandling = behandlingRepository.hent(
            behandlingId = requireNotNull(
                kontekst.forrigeBehandlingId
            ) {
                "Forrige behandling-id må finnes på revurdering"
            })
        return resultatUtleder.erRentAvslag(forrigeBehandling)
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
