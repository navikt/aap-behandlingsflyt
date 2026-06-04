package no.nav.aap.behandlingsflyt.forretningsflyt.steg

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
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingService
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

class RefusjonkravSteg private constructor(
    private val refusjonkravRepository: RefusjonkravRepository,
    private val tidligereVurderinger: TidligereVurderinger,
    private val avklaringsbehovService: AvklaringsbehovService,
    private val behandlingRepository: BehandlingRepository,
    private val behandlingService: BehandlingService,
) : BehandlingSteg {
    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        refusjonkravRepository = repositoryProvider.provide(),
        tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider, gatewayProvider),
        avklaringsbehovService = AvklaringsbehovService(repositoryProvider),
        behandlingRepository = repositoryProvider.provide(),
        behandlingService = BehandlingService(repositoryProvider, gatewayProvider),
    )

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val grunnlag = lazy { refusjonkravRepository.hentHvisEksisterer(kontekst.behandlingId) }
        val behandling = behandlingRepository.hent(kontekst.behandlingId)
        val behandlingstype = behandlingService.utledFaktiskBehandlingstype(behandling)

        avklaringsbehovService.oppdaterAvklaringsbehov(
            definisjon = Definisjon.REFUSJON_KRAV,
            vedtakBehøverVurdering = {
                when (behandlingstype) {
                    TypeBehandling.Førstegangsbehandling -> !tidligereVurderinger.girAvslagEllerIngenBehandlingsgrunnlag(
                        kontekst,
                        type()
                    )

                    else -> false
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
