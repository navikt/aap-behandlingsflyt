package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsopptrapping.ArbeidsopptrappingRepository
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

class ArbeidsopptrappingSteg private constructor(
    private val avklaringsbehovService: AvklaringsbehovService,
    private val tidligereVurderinger: TidligereVurderinger,
    private val arbeidsopptrappingRepository: ArbeidsopptrappingRepository,
) : BehandlingSteg {

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        avklaringsbehovService.oppdaterAvklaringsbehovForPeriodisertYtelsesvilkår(
            definisjon = Definisjon.ARBEIDSOPPTRAPPING,
            tvingerAvklaringsbehov = setOf(Vurderingsbehov.VURDER_ARBEIDSOPPTRAPPING),
            nårVurderingErRelevant = { nårVurderingErRelevant(it) },
            nårVurderingErGyldig = { nårVurderingErRelevant(kontekst) },
            tilbakestillGrunnlag = {
                kontekst.forrigeBehandlingId?.let {
                    arbeidsopptrappingRepository.lagre(
                        kontekst.behandlingId,
                        arbeidsopptrappingRepository.hentHvisEksisterer(it)?.vurderinger.orEmpty()
                    )
                }
            },
            gjeldendeVurderinger = {
                arbeidsopptrappingRepository.hentHvisEksisterer(kontekst.behandlingId)?.gjeldendeVurderinger().orEmpty()
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
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): BehandlingSteg {
            return ArbeidsopptrappingSteg(
                avklaringsbehovService = AvklaringsbehovService(repositoryProvider, gatewayProvider),
                tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider, gatewayProvider),
                arbeidsopptrappingRepository = repositoryProvider.provide(),
            )
        }

        override fun type(): StegType {
            return StegType.ARBEIDSOPPTRAPPING
        }
    }
}
