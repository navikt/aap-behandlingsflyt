package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
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

class SamordningArbeidsgiverSteg(
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val avklaringsbehovService: AvklaringsbehovService,
    private val tidligereVurderinger: TidligereVurderinger,
) : BehandlingSteg {
    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        avklaringsbehovService.oppdaterAvklaringsbehov(
            avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId),
            definisjon = Definisjon.SAMORDNING_ARBEIDSGIVER,
            vedtakBehøverVurdering = {
                when (kontekst.vurderingType) {
                    VurderingType.FØRSTEGANGSBEHANDLING,
                    VurderingType.REVURDERING -> {
                        when {
                            tidligereVurderinger.girAvslagEllerIngenBehandlingsgrunnlag(kontekst, type()) -> false
                            Vurderingsbehov.REVURDER_SAMORDNING_ARBEIDSGIVER in kontekst.vurderingsbehovRelevanteForSteg -> true
                            else -> kontekst.vurderingsbehovRelevanteForSteg.isNotEmpty()
                        }
                    }

                    VurderingType.MELDEKORT,
                    VurderingType.EFFEKTUER_AKTIVITETSPLIKT,
                    VurderingType.EFFEKTUER_AKTIVITETSPLIKT_11_9,
                    VurderingType.IKKE_RELEVANT -> false
                }
            },
            erTilstrekkeligVurdert = { true },
            tilbakestillGrunnlag = { },
            kontekst = kontekst
        )

        return Fullført
    }

    companion object : FlytSteg {
        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): BehandlingSteg {
            return SamordningArbeidsgiverSteg(
                avklaringsbehovRepository = repositoryProvider.provide(),
                avklaringsbehovService = AvklaringsbehovService(repositoryProvider),
                tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider)
            )
        }

        override fun type(): StegType {
            return StegType.SAMORDNING_ARBEIDSGIVER
        }
    }
}
