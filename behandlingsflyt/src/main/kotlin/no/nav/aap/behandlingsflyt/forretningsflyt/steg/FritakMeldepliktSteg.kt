package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
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
        avklaringsbehovService.oppdaterAvklaringsbehovForPeriodisertYtelsesvilkår(
            definisjon = Definisjon.FRITAK_MELDEPLIKT,
            tvingerAvklaringsbehov = setOf(Vurderingsbehov.VURDER_FRITAK_MELDEPLIKT),
            nårVurderingErRelevant = { nårVurderingErRelevant(it) },
            nårVurderingErGyldig = { nårVurderingErRelevant(kontekst) },
            tilbakestillGrunnlag = {
                kontekst.forrigeBehandlingId?.let {
                    meldepliktRepository.lagre(
                        kontekst.behandlingId,
                        meldepliktRepository.hentHvisEksisterer(it)?.vurderinger.orEmpty()
                    )
                }
            },
            gjeldendeVurderinger = {
                meldepliktRepository.hentHvisEksisterer(kontekst.behandlingId)?.gjeldendeVurderinger().orEmpty()
            },
            kontekst = kontekst,
        )
        return Fullført
    }

    private fun nårVurderingErRelevant(
        kontekst: FlytKontekstMedPerioder
    ): Tidslinje<Boolean> {
        return tidligereVurderinger.behandlingsutfall(kontekst, type()).map { utfall ->
            when (utfall) {
                TidligereVurderinger.IkkeBehandlingsgrunnlag, TidligereVurderinger.UunngåeligAvslag -> false

                is TidligereVurderinger.PotensieltOppfylt -> true
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
