package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.uførevurdering.SamordningUføreRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.uførevurdering.SamordningUføreVurderingPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.uførevurdering.SamordningUføreVurderingValidator
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.UføreRepository
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

class SamordningUføreSteg(
    private val samordningUføreRepository: SamordningUføreRepository,
    private val uføreRepository: UføreRepository,
    private val tidligereVurderinger: TidligereVurderinger,
    private val avklaringsbehovService: AvklaringsbehovService
) : BehandlingSteg {
    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        samordningUføreRepository = repositoryProvider.provide(),
        uføreRepository = repositoryProvider.provide(),
        tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider, gatewayProvider),
        avklaringsbehovService = AvklaringsbehovService(repositoryProvider, gatewayProvider)
    )

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        avklaringsbehovService.oppdaterAvklaringsbehov(
            definisjon = Definisjon.AVKLAR_SAMORDNING_UFØRE,
            vedtakBehøverVurdering = {
                when (kontekst.vurderingType) {
                    VurderingType.FØRSTEGANGSBEHANDLING,
                    VurderingType.REVURDERING,
                    VurderingType.MIGERING_FRA_ARENA -> {
                        when {
                            tidligereVurderinger.girAvslagEllerIngenBehandlingsgrunnlag(kontekst, type()) -> false
                            vedtakBehøverVurdering(kontekst) -> true
                            else -> erManueltTriggetRevurdering(kontekst)
                        }
                    }

                    VurderingType.MELDEKORT,
                    VurderingType.AUTOMATISK_BREV,
                    VurderingType.UTVID_VEDTAKSLENGDE,
                    VurderingType.MIGRER_RETTIGHETSPERIODE,
                    VurderingType.EFFEKTUER_AKTIVITETSPLIKT,
                    VurderingType.EFFEKTUER_AKTIVITETSPLIKT_11_9,
                    VurderingType.G_REGULERING,
                    VurderingType.OVERGANG_UFORE_STANS,
                    VurderingType.IKKE_RELEVANT -> {
                        false
                    }
                }

            },
            erTilstrekkeligVurdert = {
                harVurdertAllePerioder(kontekst)
            },
            tilbakestillGrunnlag = {
                kontekst.forrigeBehandlingId
                    ?.let { samordningUføreRepository.hentHvisEksisterer(it)?.vurdering }
                    ?.also { samordningUføreRepository.lagre(kontekst.behandlingId, it) }
            },
            kontekst = kontekst

        )

        return Fullført
    }

    private fun vedtakBehøverVurdering(kontekst: FlytKontekstMedPerioder): Boolean {
        val vurderingerIForrigeBehandling =
            kontekst.forrigeBehandlingId?.let { samordningUføreRepository.hentHvisEksisterer(it) }?.vurdering?.vurderingPerioder.orEmpty()
        return manglendeVurderinger(kontekst, vurderingerIForrigeBehandling).isNotEmpty()
    }

    private fun harVurdertAllePerioder(kontekst: FlytKontekstMedPerioder): Boolean {
        val vurderinger = samordningUføreRepository.hentHvisEksisterer(kontekst.behandlingId)
            ?.vurdering?.vurderingPerioder.orEmpty()
        return manglendeVurderinger(kontekst, vurderinger).isEmpty()
    }

    private fun manglendeVurderinger(
        kontekst: FlytKontekstMedPerioder,
        vurderinger: List<SamordningUføreVurderingPeriode>
    ) = SamordningUføreVurderingValidator.manglendeVurderinger(
        behandlingsutfall = tidligereVurderinger.behandlingsutfall(kontekst, type()),
        uførevedtak = uføreRepository.hentHvisEksisterer(kontekst.behandlingId)?.vedtak.orEmpty(),
        vurderinger = vurderinger,
    )

    private fun erManueltTriggetRevurdering(
        kontekst: FlytKontekstMedPerioder,
    ): Boolean {
        return kontekst.erRevurderingMedVurderingsbehov(Vurderingsbehov.REVURDER_SAMORDNING_UFØRE)
    }


    companion object : FlytSteg {
        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): BehandlingSteg {
            return SamordningUføreSteg(repositoryProvider, gatewayProvider)
        }

        override fun type(): StegType {
            return StegType.SAMORDNING_UFØRE
        }
    }
}
