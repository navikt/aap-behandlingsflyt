package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovMetadataUtleder
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.behandling.oppholdskrav.OppholdskravGrunnlagRepository
import no.nav.aap.behandlingsflyt.behandling.oppholdskrav.tilTidslinje
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.behandling.vilkår.oppholdskrav.Oppholdskravvilkår
import no.nav.aap.behandlingsflyt.behandling.vilkår.oppholdskrav.OppholdskravvilkårGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
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
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.orEmpty
import no.nav.aap.lookup.repository.RepositoryProvider

class VurderOppholdskravSteg private constructor(
    private val oppholdskravGrunnlagRepository: OppholdskravGrunnlagRepository,
    private val avklaringsbehovService: AvklaringsbehovService,
    private val tidligereVurderinger: TidligereVurderinger,
    private val vilkårsresultatRepository: VilkårsresultatRepository
) : BehandlingSteg, AvklaringsbehovMetadataUtleder {

    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        oppholdskravGrunnlagRepository = repositoryProvider.provide(),
        avklaringsbehovService = AvklaringsbehovService(repositoryProvider),
        tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider, gatewayProvider),
        vilkårsresultatRepository = repositoryProvider.provide(),
    )

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        avklaringsbehovService.oppdaterAvklaringsbehovForPeriodisertYtelsesvilkår(
            kontekst = kontekst,
            definisjon = Definisjon.AVKLAR_OPPHOLDSKRAV,
            tvingerAvklaringsbehov = setOf(Vurderingsbehov.OPPHOLDSKRAV),
            nårVurderingErRelevant = { nyKontekst -> nårVurderingErRelevant(nyKontekst) },
            nårVurderingErGyldig = { nårVurderingErGyldig(kontekst) },
            tilbakestillGrunnlag = {
                oppholdskravGrunnlagRepository.tilbakestillGrunnlag(
                    kontekst.behandlingId,
                    kontekst.forrigeBehandlingId
                )
            },
        )

        when (kontekst.vurderingType) {
            VurderingType.FØRSTEGANGSBEHANDLING,
            VurderingType.MIGRER_RETTIGHETSPERIODE,
            VurderingType.REVURDERING -> {
                val vilkårsresultat = vilkårsresultatRepository.hent(kontekst.behandlingId)
                Oppholdskravvilkår(vilkårsresultat).vurder(OppholdskravvilkårGrunnlag(
                    oppholdskravGrunnlag = oppholdskravGrunnlagRepository.hentHvisEksisterer(kontekst.behandlingId),
                    vurderFra = kontekst.rettighetsperiode.fom,
                ))
                vilkårsresultatRepository.lagre(kontekst.behandlingId, vilkårsresultat)
            }

            VurderingType.MELDEKORT,
            VurderingType.UTVID_VEDTAKSLENGDE,
            VurderingType.AUTOMATISK_BREV,
            VurderingType.EFFEKTUER_AKTIVITETSPLIKT,
            VurderingType.EFFEKTUER_AKTIVITETSPLIKT_11_9,
            VurderingType.IKKE_RELEVANT -> {
                /* noop */
            }
        }
        return Fullført
    }

    override fun nårVurderingErRelevant(
        kontekst: FlytKontekstMedPerioder,
    ): Tidslinje<Boolean> {
        val tidligereVurderingsutfall = tidligereVurderinger.behandlingsutfall(kontekst, type())
        return tidligereVurderingsutfall.mapValue { behandlingsutfall ->
            when (behandlingsutfall) {
                TidligereVurderinger.IkkeBehandlingsgrunnlag -> false
                TidligereVurderinger.UunngåeligAvslag -> false
                is TidligereVurderinger.PotensieltOppfylt -> true
            }
        }
    }

    private fun nårVurderingErGyldig(
        kontekst: FlytKontekstMedPerioder,
    ): Tidslinje<Boolean> {
        val grunnlag = oppholdskravGrunnlagRepository.hentHvisEksisterer(kontekst.behandlingId)
        return grunnlag?.vurderinger?.tilTidslinje().orEmpty().mapValue { true }
    }

    override val stegType = type()

    companion object : FlytSteg {
        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): VurderOppholdskravSteg {
            return VurderOppholdskravSteg(repositoryProvider, gatewayProvider)
        }

        override fun type(): StegType {
            return StegType.VURDER_OPPHOLDSKRAV
        }
    }
}
