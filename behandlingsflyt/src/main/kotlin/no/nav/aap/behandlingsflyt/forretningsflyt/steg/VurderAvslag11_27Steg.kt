package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.behandling.avslag11_27.Avslag11_27Repository
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

class VurderAvslag11_27Steg private constructor(
    private val avslag11_27repository: Avslag11_27Repository,
    private val avklaringsbehovService: AvklaringsbehovService,
    private val tidligereVurderinger: TidligereVurderinger
) : BehandlingSteg {

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        avklaringsbehovService.oppdaterAvklaringsbehov(
            kontekst = kontekst,
            definisjon = Definisjon.VURDER_AVSLAG_11_27,
            vedtakBehøverVurdering = { vedtakBehøverVurdering(kontekst) },
            erTilstrekkeligVurdert = {
                avslag11_27repository.hentHvisEksisterer(kontekst.behandlingId) != null
            },
            tilbakestillGrunnlag = { tilbakestillGrunnlag(kontekst) },
        )
        return Fullført
    }

    private fun vedtakBehøverVurdering(kontekst: FlytKontekstMedPerioder): Boolean {
        return when (kontekst.vurderingType) {
            VurderingType.FØRSTEGANGSBEHANDLING,
            VurderingType.REVURDERING ->
                (tidligereVurderinger.muligMedRettTilAAP(kontekst, type())
                        && Vurderingsbehov.VURDER_AVSLAG_11_27 in kontekst.vurderingsbehovRelevanteForSteg
                        && kontekst.vurderingsbehovRelevanteForSteg.isNotEmpty())

            VurderingType.MELDEKORT,
            VurderingType.AUTOMATISK_BREV,
            VurderingType.UTVID_VEDTAKSLENGDE,
            VurderingType.MIGRER_RETTIGHETSPERIODE,
            VurderingType.EFFEKTUER_AKTIVITETSPLIKT,
            VurderingType.EFFEKTUER_AKTIVITETSPLIKT_11_9,
            VurderingType.G_REGULERING,
            VurderingType.OVERGANG_UFORE_STANS,
            VurderingType.IKKE_RELEVANT ->
                false
        }
    }

    private fun tilbakestillGrunnlag(kontekst: FlytKontekstMedPerioder) {
        val tidligereVurderinger =
            kontekst.forrigeBehandlingId?.let { avslag11_27repository.hentHvisEksisterer(it)?.vurderinger } ?: emptyList()

        val alleVurderinger =
            avslag11_27repository.hentHvisEksisterer(kontekst.behandlingId)?.vurderinger ?: emptyList()

        if (tidligereVurderinger != alleVurderinger) { // TODO Thao: Test ut om dette stemmer
            avslag11_27repository.lagre(kontekst.behandlingId, tidligereVurderinger)
        }
    }

    companion object : FlytSteg {
        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): BehandlingSteg {
            return VurderAvslag11_27Steg(
                avslag11_27repository = repositoryProvider.provide(),
                avklaringsbehovService = AvklaringsbehovService(repositoryProvider),
                tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider, gatewayProvider)
            )
        }

        override fun type(): StegType {
            return StegType.VURDER_AVSLAG_11_27
        }
    }
}
