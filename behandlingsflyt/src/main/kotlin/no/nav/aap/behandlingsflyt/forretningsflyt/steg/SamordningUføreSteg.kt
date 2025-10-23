package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.uførevurdering.SamordningUføreRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.UføreGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.UføreRepository
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

class SamordningUføreSteg(
    private val samordningUføreRepository: SamordningUføreRepository,
    private val uføreRepository: UføreRepository,
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val tidligereVurderinger: TidligereVurderinger,
    private val avklaringsbehovService: AvklaringsbehovService
) : BehandlingSteg {
    constructor(repositoryProvider: RepositoryProvider) : this(
        samordningUføreRepository = repositoryProvider.provide(),
        uføreRepository = repositoryProvider.provide(),
        avklaringsbehovRepository = repositoryProvider.provide(),
        tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider),
        avklaringsbehovService = AvklaringsbehovService(repositoryProvider)
    )

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)

        avklaringsbehovService.oppdaterAvklaringsbehov(
            avklaringsbehovene = avklaringsbehovene,
            definisjon = Definisjon.AVKLAR_SAMORDNING_UFØRE,
            vedtakBehøverVurdering = {
                when (kontekst.vurderingType) {
                    VurderingType.FØRSTEGANGSBEHANDLING, VurderingType.REVURDERING -> {
                        vedtakBehøverVurdering(kontekst) || erManueltTriggetRevurdering(kontekst)
                    }

                    VurderingType.MELDEKORT,
                    VurderingType.EFFEKTUER_AKTIVITETSPLIKT,
                    VurderingType.EFFEKTUER_AKTIVITETSPLIKT_11_9,
                    VurderingType.IKKE_RELEVANT -> {
                        false
                    }
                }

            },
            erTilstrekkeligVurdert = {
                harVurdertAllePerioder(kontekst.behandlingId)
            },
            tilbakestillGrunnlag = {
                samordningUføreRepository.slett(kontekst.behandlingId)
            },
            kontekst = kontekst

        )

        return Fullført
    }

    private fun vedtakBehøverVurdering(kontekst: FlytKontekstMedPerioder): Boolean {
        if (tidligereVurderinger.girAvslagEllerIngenBehandlingsgrunnlag(
                kontekst,
                type()
            )
        ) {
            return false
        }

        val tidligereVurderinger =
            kontekst.forrigeBehandlingId?.let { samordningUføreRepository.hentHvisEksisterer(it) }?.vurdering?.vurderingPerioder.orEmpty()
        val uføreGrunnlag = hentVurderingPerioder(kontekst.behandlingId)?.vurderinger.orEmpty()
        return uføreGrunnlag.any { uføre ->
            tidligereVurderinger.none { vurdering -> vurdering.virkningstidspunkt == uføre.virkningstidspunkt }
        }

    }


    private fun hentVurderingPerioder(behandlingId: BehandlingId): UføreGrunnlag? {
        val uføreGrunnlag = uføreRepository.hentHvisEksisterer(behandlingId = behandlingId)
        return uføreGrunnlag
    }

    private fun harVurdertAllePerioder(behandlingId: BehandlingId): Boolean {
        val uføreGrunnlag = uføreRepository.hentHvisEksisterer(behandlingId = behandlingId)
        val samordningUføreGrunnlag = samordningUføreRepository.hentHvisEksisterer(behandlingId)
        val vurderinger = samordningUføreGrunnlag?.vurdering?.vurderingPerioder
        return uføreGrunnlag?.vurderinger?.all { uføre ->
            vurderinger?.any { vurdering -> vurdering.virkningstidspunkt == uføre.virkningstidspunkt } ?: false
        } ?: true
    }

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
            return SamordningUføreSteg(repositoryProvider)
        }

        override fun type(): StegType {
            return StegType.SAMORDNING_UFØRE
        }
    }
}
