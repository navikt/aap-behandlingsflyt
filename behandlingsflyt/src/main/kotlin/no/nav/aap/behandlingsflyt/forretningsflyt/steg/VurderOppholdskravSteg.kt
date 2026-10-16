package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.arena.ArenaMigreringMapper
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovMetadataUtleder
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.behandling.oppholdskrav.OppholdskravGrunnlagRepository
import no.nav.aap.behandlingsflyt.behandling.oppholdskrav.tilTidslinje
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.behandling.vilkår.oppholdskrav.Oppholdskravvilkår
import no.nav.aap.behandlingsflyt.behandling.vilkår.oppholdskrav.OppholdskravvilkårGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårService
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.MigrerVurderingFraArena
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.orEmpty
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.PeriodisertVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.gjeldendeVurderinger
import no.nav.aap.lookup.repository.RepositoryProvider

class VurderOppholdskravSteg private constructor(
    private val oppholdskravGrunnlagRepository: OppholdskravGrunnlagRepository,
    private val avklaringsbehovService: AvklaringsbehovService,
    private val tidligereVurderinger: TidligereVurderinger,
    private val vilkårService: VilkårService,
    private val unleashGateway: UnleashGateway,
) : BehandlingSteg, AvklaringsbehovMetadataUtleder, MigrerVurderingFraArena {

    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        oppholdskravGrunnlagRepository = repositoryProvider.provide(),
        avklaringsbehovService = AvklaringsbehovService(repositoryProvider, gatewayProvider),
        tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider, gatewayProvider),
        vilkårService = VilkårService(repositoryProvider),
        unleashGateway = gatewayProvider.provide(),
    )

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        if (kontekst.erMigreringFraArena() && unleashGateway.isEnabled(BehandlingsflytFeature.MigrererOppholdskravFraArenaAutomatisk)) {
            val harRelevantePerioderForMigrering = nårVurderingErRelevant(kontekst).any { it }
            if (harRelevantePerioderForMigrering) {
                migrerVurderingFraArena(kontekst)
            }
        }

        avklaringsbehovService.oppdaterAvklaringsbehovForPeriodisertYtelsesvilkår(
            kontekst = kontekst,
            definisjon = Definisjon.AVKLAR_OPPHOLDSKRAV,
            tvingerAvklaringsbehov = setOf(
                Vurderingsbehov.OPPHOLDSKRAV,
                Vurderingsbehov.AVVIST_SOKNAD_OM_AAP_UNDER_OPPHOLD_I_UTLANDET
            ),
            nårVurderingErRelevant = { nyKontekst -> nårVurderingErRelevant(nyKontekst) },
            nårVurderingErGyldig = { nårVurderingErGyldig(kontekst) },
            gjeldendeVurderinger = { hentGjeldendeVurderinger(kontekst) },
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
            VurderingType.REVURDERING,
            VurderingType.MIGERING_FRA_ARENA -> {
                vilkårService.vurderVilkår(
                    behandlingId = kontekst.behandlingId,
                    faktagrunnlag = OppholdskravvilkårGrunnlag(
                        oppholdskravGrunnlag = oppholdskravGrunnlagRepository.hentHvisEksisterer(kontekst.behandlingId),
                        vurderFra = kontekst.rettighetsperiode.fom,
                    ),
                    vilkårsvurderer = Oppholdskravvilkår,
                )
            }

            VurderingType.MELDEKORT,
            VurderingType.UTVID_VEDTAKSLENGDE,
            VurderingType.AUTOMATISK_BREV,
            VurderingType.EFFEKTUER_AKTIVITETSPLIKT,
            VurderingType.EFFEKTUER_AKTIVITETSPLIKT_11_9,
            VurderingType.G_REGULERING,
            VurderingType.OVERGANG_UFORE_STANS,
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
                is TidligereVurderinger.UunngåeligAvslag -> false
                is TidligereVurderinger.PotensieltOppfylt -> true
            }
        }
    }

    private fun hentGjeldendeVurderinger(kontekst: FlytKontekstMedPerioder): Tidslinje<PeriodisertVurdering> {
        return oppholdskravGrunnlagRepository.hentHvisEksisterer(kontekst.behandlingId)
            ?.somPeriodiserteVurderinger()
            ?.gjeldendeVurderinger()
            ?.mapValue { it as PeriodisertVurdering }
            .orEmpty()
    }

    private fun nårVurderingErGyldig(
        kontekst: FlytKontekstMedPerioder,
    ): Tidslinje<Boolean> {
        val grunnlag = oppholdskravGrunnlagRepository.hentHvisEksisterer(kontekst.behandlingId)
        return grunnlag?.vurderinger?.tilTidslinje().orEmpty().mapValue { true }
    }

    override fun migrerVurderingFraArena(kontekst: FlytKontekstMedPerioder) {
        require(kontekst.erMigreringFraArena()) {
            "Kan ikke migrere vurdering fra Arena for sak ${kontekst.sakId} fordi vurderingstype ikke er migrering"
        }

        oppholdskravGrunnlagRepository.lagre(
            behandlingId = kontekst.behandlingId,
            oppholdskravVurdering = ArenaMigreringMapper.mapOppfyltOppholdskravVurdering(
                behandlingId = kontekst.behandlingId,
                fom = kontekst.rettighetsperiode.fom,
            ),
        )
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
