package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.rettighetstype.KvoteOk
import no.nav.aap.behandlingsflyt.behandling.rettighetstype.utledStansEllerOpphør
import no.nav.aap.behandlingsflyt.behandling.rettighetstype.vurderRettighetstypeOgKvoter
import no.nav.aap.behandlingsflyt.behandling.underveis.KvoteService
import no.nav.aap.behandlingsflyt.behandling.underveis.Kvoter
import no.nav.aap.behandlingsflyt.behandling.vilkår.kvote.OrdinærKvoteFaktagrunnlag
import no.nav.aap.behandlingsflyt.behandling.vilkår.kvote.OrdinærKvoteVilkår
import no.nav.aap.behandlingsflyt.behandling.vilkår.kvote.SykepengeerstatningKvoteFaktagrunnlag
import no.nav.aap.behandlingsflyt.behandling.vilkår.kvote.SykepengeerstatningKvoteVilkår
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.rettighetstype.RettighetstypeFaktagrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.rettighetstype.RettighetstypeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.stansopphør.Opphør
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.stansopphør.Stans
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.stansopphør.StansOpphørGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.stansopphør.StansOpphørRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.ApplikasjonsVersjon
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.lookup.repository.RepositoryProvider
import org.slf4j.LoggerFactory

class RettighetstypeSteg(
    val rettighetstypeRepository: RettighetstypeRepository,
    val vilkårsresultatRepository: VilkårsresultatRepository,
    val stansOpphørRepository: StansOpphørRepository,
    val unleashGateway: UnleashGateway,
    val kvoteService: KvoteService,
) : BehandlingSteg {
    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        rettighetstypeRepository = repositoryProvider.provide(),
        vilkårsresultatRepository = repositoryProvider.provide(),
        stansOpphørRepository = repositoryProvider.provide(),
        unleashGateway = gatewayProvider.provide(),
        kvoteService = KvoteService(),
    )

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val behandlingId = kontekst.behandlingId

        val vilkårsresultat = vilkårsresultatRepository.hent(behandlingId)

        val kvoter = kvoteService.beregn()
        val kvotevurdering = vurderRettighetstypeOgKvoter(vilkårsresultat, kvoter)

        val rettighetstypeJustertForKvote =
            kvotevurdering
                .filter { it.verdi is KvoteOk }
                .mapNotNull { it.rettighetsType }.komprimer()

        val faktagrunnlag = RettighetstypeFaktagrunnlag(vilkårsresultat)
        rettighetstypeRepository.lagre(
            behandlingId,
            rettighetstypeJustertForKvote,
            faktagrunnlag,
            ApplikasjonsVersjon.versjon
        )

        /** Kvotene lagres som vilkårsvurderinger, men er ikke implementert som Krav ved vurdering av rettighetstype,
         *  ettersom kravspesfikasjonen er input til kvotevurderingen
         */
        OrdinærKvoteVilkår(vilkårsresultat).vurder(OrdinærKvoteFaktagrunnlag(kvotevurdering, kvoter))

        SykepengeerstatningKvoteVilkår(vilkårsresultat).vurder(
            SykepengeerstatningKvoteFaktagrunnlag(
                kvotevurdering,
                kvoter
            )
        )
        vilkårsresultatRepository.lagre(kontekst.behandlingId, vilkårsresultat)

        when (kontekst.vurderingType) {
            /* Følgende påvirker rettighetstypen / stans / opphør. */
            VurderingType.FØRSTEGANGSBEHANDLING,
            VurderingType.OVERGANG_UFORE_STANS,
            VurderingType.REVURDERING,
            VurderingType.UTVID_VEDTAKSLENGDE,
            VurderingType.MIGRER_RETTIGHETSPERIODE,
            VurderingType.EFFEKTUER_AKTIVITETSPLIKT ->
                lagreStansOgOpphør(
                    kontekst,
                    rettighetstypeJustertForKvote,
                    vilkårsresultat,
                    kvoter,
                )

            /* Følgende påvirker ikke rettighetstypen / stans / opphør. */
            VurderingType.MELDEKORT,
            VurderingType.AUTOMATISK_BREV,
            VurderingType.EFFEKTUER_AKTIVITETSPLIKT_11_9,
            VurderingType.G_REGULERING,
            VurderingType.IKKE_RELEVANT -> {
            }
        }

        return Fullført
    }

    fun lagreStansOgOpphør(
        kontekst: FlytKontekstMedPerioder,
        rettighetstyper: Tidslinje<RettighetsType>,
        vilkårsresultat: Vilkårsresultat,
        kvoter: Kvoter,
    ) {
        val forrigeGrunnlag = kontekst.forrigeBehandlingId?.let { stansOpphørRepository.hentHvisEksisterer(it) }
            ?: when (kontekst.behandlingType) {
                TypeBehandling.Førstegangsbehandling -> StansOpphørGrunnlag(emptySet(), emptyMap(), null)
                TypeBehandling.Revurdering -> return
                else -> return
            }

        val stansOpphørGrunnlag = forrigeGrunnlag.utledNyttGrunnlag(
            utledStansEllerOpphør(
                vilkårsresultat,
                kvoter,
                kontekst.rettighetsperiode
            ),
            kontekst.behandlingId
        )
        validerStansOpphør(stansOpphørGrunnlag, rettighetstyper)

        stansOpphørRepository.lagre(kontekst.behandlingId, stansOpphørGrunnlag)
    }

    companion object : FlytSteg {
        private val log = LoggerFactory.getLogger(javaClass)

        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): BehandlingSteg {
            return RettighetstypeSteg(repositoryProvider, gatewayProvider)
        }

        override fun type(): StegType {
            return StegType.FASTSETT_RETTIGHETSTYPE
        }

        fun validerStansOpphør(
            grunnlag: StansOpphørGrunnlag,
            rettighetstyper: Tidslinje<RettighetsType>,
        ): Boolean {
            var ok = true
            if (grunnlag.stansOpphørV2 != null) {
                val stansOpphørV1 = grunnlag.gjeldendeStansOgOpphør().associate {
                    it.fom to when (it.vurdering) {
                        is Opphør -> Opphør(it.vurdering.årsaker)
                        is Stans -> Stans(it.vurdering.årsaker)
                    }
                }
                if (stansOpphørV1 != grunnlag.stansOpphørV2) {
                    log.warn(
                        "stansOpphørV2 samsvarer ikke med gjeldendeStansOgOpphør() V1={} V2={}",
                        stansOpphørV1,
                        grunnlag.stansOpphørV2
                    )
                    ok = false
                }

                for (fom in grunnlag.stansOpphørV2.keys) {
                    val rettighetstypePåStansOpphørDag = rettighetstyper.segment(fom)
                    if (rettighetstypePåStansOpphørDag != null) {
                        log.warn(
                            "har rett {} til AAP på samme dag som stans/opphør {}",
                            rettighetstypePåStansOpphørDag.verdi,
                            fom
                        )
                        ok = false
                    }

                    val rettFørFom = rettighetstyper.segment(fom.minusDays(1))
                    if (rettFørFom == null) {
                        log.warn("har stans/opphør på {}, men har ikke rett dagen før heller", fom)
                        ok = false
                    }
                }
            }
            return ok
        }
    }
}