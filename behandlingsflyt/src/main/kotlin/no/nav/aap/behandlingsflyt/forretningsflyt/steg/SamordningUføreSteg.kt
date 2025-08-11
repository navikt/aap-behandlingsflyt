package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.uførevurdering.SamordningUføreRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.UføreRepository
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FantAvklaringsbehov
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

class SamordningUføreSteg(
    private val samordningUføreRepository: SamordningUføreRepository,
    private val uføreRepository: UføreRepository,
    private val behandlingRepository: BehandlingRepository,
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val tidligereVurderinger: TidligereVurderinger,
) : BehandlingSteg {
    constructor(repositoryProvider: RepositoryProvider) : this(
        samordningUføreRepository = repositoryProvider.provide(),
        uføreRepository = repositoryProvider.provide(),
        behandlingRepository = repositoryProvider.provide(),
        avklaringsbehovRepository = repositoryProvider.provide(),
        tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider),
    )

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)

        when (kontekst.vurderingType) {
            VurderingType.FØRSTEGANGSBEHANDLING -> {
                if (tidligereVurderinger.girAvslagEllerIngenBehandlingsgrunnlag(kontekst, type())) {
                    avklaringsbehovene.avbrytForSteg(type())
                    return Fullført
                }

                if (erIkkeVurdertTidligereIBehandlingen(avklaringsbehovene) || !harVurdertAllePerioder(kontekst.behandlingId)) {
                    val uføreGrunnlag = uføreRepository.hentHvisEksisterer(kontekst.behandlingId)
                    if (uføreGrunnlag != null) {
                        return FantAvklaringsbehov(Definisjon.AVKLAR_SAMORDNING_UFØRE)
                    }
                }
            }

            VurderingType.REVURDERING -> {
                val uføreGrunnlag = uføreRepository.hentHvisEksisterer(kontekst.behandlingId)
                val behandling = behandlingRepository.hent(kontekst.behandlingId)
                val uføreGrunnlagPåForrigeBehandling = behandling.forrigeBehandlingId?.let {
                    uføreRepository.hentHvisEksisterer(it)
                }

                val harEndringerFraForrigeBehandling =
                    uføreGrunnlag?.vurderinger != uføreGrunnlagPåForrigeBehandling?.vurderinger
                val manglerVurdering = erIkkeVurdertTidligereIBehandlingen(avklaringsbehovene) &&
                        harEndringerFraForrigeBehandling
                if (manglerVurdering || !harVurdertAllePerioder(kontekst.behandlingId)) {
                    return FantAvklaringsbehov(Definisjon.AVKLAR_SAMORDNING_UFØRE)
                }
            }

            VurderingType.MELDEKORT,
            VurderingType.IKKE_RELEVANT -> {}
        }
        return Fullført
    }

    private fun harVurdertAllePerioder(behandlingId: BehandlingId): Boolean {
        val uføreGrunnlag = uføreRepository.hentHvisEksisterer(behandlingId = behandlingId)
        val samordningUføreGrunnlag = samordningUføreRepository.hentHvisEksisterer(behandlingId)
        val vurderinger = samordningUføreGrunnlag?.vurdering?.vurderingPerioder
        return uføreGrunnlag?.vurderinger?.all { uføre ->
            vurderinger?.any { vurdering -> vurdering.virkningstidspunkt == uføre.virkningstidspunkt } ?: false
        } ?: true
    }

    private fun erIkkeVurdertTidligereIBehandlingen(
        avklaringsbehovene: Avklaringsbehovene
    ): Boolean {
        return !avklaringsbehovene.erVurdertTidligereIBehandlingen(Definisjon.AVKLAR_SAMORDNING_UFØRE)
    }

    companion object : FlytSteg {
        override fun konstruer(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider): BehandlingSteg {
            return SamordningUføreSteg(repositoryProvider)
        }

        override fun type(): StegType {
            return StegType.SAMORDNING_UFØRE
        }
    }
}
