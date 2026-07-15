package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsevne.ArbeidsevneRepository
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.tidslinje.orEmpty
import no.nav.aap.lookup.repository.RepositoryProvider

class FastsettArbeidsevneSteg private constructor(
    private val avklaringsbehovService: AvklaringsbehovService,
    private val tidligereVurderinger: TidligereVurderinger,
    private val arbeidsevneRepository: ArbeidsevneRepository,
) : BehandlingSteg {

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val vurderinger = arbeidsevneRepository.hentHvisEksisterer(kontekst.behandlingId)?.tilTidslinje().orEmpty()

        avklaringsbehovService.oppdaterAvklaringsbehovForPeriodisertYtelsesvilkår(
            definisjon = Definisjon.FASTSETT_ARBEIDSEVNE,
            tvingerAvklaringsbehov = setOf(Vurderingsbehov.FASTSETT_ARBEIDSEVNE),
            nårVurderingErRelevant = {
                tidligereVurderinger.behandlingsutfall(kontekst, type())
                    .leftJoin(vurderinger) { utfall, arbeidsevnevurdering ->
                        when (utfall) {
                            TidligereVurderinger.IkkeBehandlingsgrunnlag, TidligereVurderinger.UunngåeligAvslag -> false

                            is TidligereVurderinger.PotensieltOppfylt -> arbeidsevnevurdering != null
                        }
                    }
            },
            nårVurderingErGyldig = { vurderinger.mapValue { true } },
            tilbakestillGrunnlag = {},
            kontekst = kontekst,
        )
        return Fullført
    }

    companion object : FlytSteg {
        override fun konstruer(
            repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider
        ): BehandlingSteg {
            return FastsettArbeidsevneSteg(
                avklaringsbehovService = AvklaringsbehovService(
                    repositoryProvider, gatewayProvider
                ),
                tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider, gatewayProvider),
                arbeidsevneRepository = repositoryProvider.provide(),
            )
        }

        override fun type(): StegType {
            return StegType.FASTSETT_ARBEIDSEVNE
        }
    }
}
