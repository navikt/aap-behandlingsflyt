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
import no.nav.aap.komponenter.tidslinje.orEmpty
import no.nav.aap.lookup.repository.RepositoryProvider

class FritakMeldepliktSteg private constructor(
    private val avklaringsbehovService: AvklaringsbehovService,
    private val tidligereVurderinger: TidligereVurderinger,
    private val meldepliktRepository: MeldepliktRepository,
) : BehandlingSteg {

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val vurderinger = meldepliktRepository.hentHvisEksisterer(kontekst.behandlingId)?.tilTidslinje().orEmpty()

        avklaringsbehovService.oppdaterFrivilligAvklaringsbehovForPeriodisertYtelsesvilkår(
            definisjon = Definisjon.FRITAK_MELDEPLIKT,
            tvingerAvklaringsbehov = setOf(Vurderingsbehov.FRITAK_MELDEPLIKT),
            nårVurderingErRelevant = {
                tidligereVurderinger.behandlingsutfall(kontekst, FastsettArbeidsevneSteg.type())
                    .leftJoin(vurderinger) { utfall, meldepliktvurdering ->
                        when (utfall) {
                            TidligereVurderinger.IkkeBehandlingsgrunnlag,
                            TidligereVurderinger.UunngåeligAvslag -> false

                            is TidligereVurderinger.PotensieltOppfylt -> meldepliktvurdering != null
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
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
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
