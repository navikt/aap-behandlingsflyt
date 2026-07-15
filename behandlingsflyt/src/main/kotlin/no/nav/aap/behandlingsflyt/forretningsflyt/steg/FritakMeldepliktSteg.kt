package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.Fritaksvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.MeldepliktRepository
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.orEmpty
import no.nav.aap.lookup.repository.RepositoryProvider

class FritakMeldepliktSteg internal constructor(
    private val avklaringsbehovService: AvklaringsbehovService,
    private val tidligereVurderinger: TidligereVurderinger,
    private val meldepliktRepository: MeldepliktRepository,
) : BehandlingSteg {

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val vurderinger = meldepliktRepository.hentHvisEksisterer(kontekst.behandlingId)?.tilTidslinje().orEmpty()

        avklaringsbehovService.oppdaterAvklaringsbehovForPeriodisertYtelsesvilkår(
            definisjon = Definisjon.FRITAK_MELDEPLIKT,
            tvingerAvklaringsbehov = setOf(Vurderingsbehov.FRITAK_MELDEPLIKT),
            nårVurderingErRelevant = { nårVurderingErRelevant(kontekst, vurderinger) },
            nårVurderingErGyldig = {
                val nyVurdering = meldepliktRepository.hentHvisEksisterer(kontekst.behandlingId)
                    ?.vurderinger.orEmpty()
                    .filter { it.vurdertIBehandling == kontekst.behandlingId }

                val datoVurderingsbehov =
                    kontekst.vurderingsbehovRelevanteForStegMedPerioder
                        .filter { it.type == Vurderingsbehov.FRITAK_MELDEPLIKT }
                        .maxByOrNull { it.oppdatertTid }
                        ?.oppdatertTid

                nårVurderingErRelevant(kontekst, vurderinger)
                    .map { it && nyVurdering.isNotEmpty() && nyVurdering.all { vurdering -> vurdering.opprettetTid >= datoVurderingsbehov } }
            },
            tilbakestillGrunnlag = {
                kontekst.forrigeBehandlingId?.let {
                    meldepliktRepository.lagre(
                        kontekst.behandlingId,
                        meldepliktRepository.hentHvisEksisterer(it)?.vurderinger.orEmpty()
                    )
                }
            },
            kontekst = kontekst,
        )
        return Fullført
    }

    private fun nårVurderingErRelevant(
        kontekst: FlytKontekstMedPerioder,
        vurderinger: Tidslinje<Fritaksvurdering.FritaksvurderingData>
    ): Tidslinje<Boolean> {
        val finnesVurderingsbehov =
            kontekst.vurderingsbehovRelevanteForSteg.any { it == Vurderingsbehov.FRITAK_MELDEPLIKT }

        return Tidslinje.map2(
            tidligereVurderinger.behandlingsutfall(kontekst, type()), vurderinger
        ) { utfall, meldepliktvurdering ->
            when (utfall) {
                TidligereVurderinger.IkkeBehandlingsgrunnlag, TidligereVurderinger.UunngåeligAvslag -> false

                is TidligereVurderinger.PotensieltOppfylt, null -> meldepliktvurdering != null || finnesVurderingsbehov
            }
        }
    }

    companion object : FlytSteg {
        override fun konstruer(
            repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider
        ): BehandlingSteg {
            return FritakMeldepliktSteg(
                avklaringsbehovService = AvklaringsbehovService(repositoryProvider, gatewayProvider),
                tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider, gatewayProvider),
                meldepliktRepository = repositoryProvider.provide(),
            )
        }

        override fun type(): StegType {
            return StegType.FRITAK_MELDEPLIKT
        }
    }
}
