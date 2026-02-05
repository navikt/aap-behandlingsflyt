package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.rettighetstype.vurderRettighetstypeOgKvoter
import no.nav.aap.behandlingsflyt.behandling.underveis.KvoteService
import no.nav.aap.behandlingsflyt.behandling.vilkår.kvote.OrdinærKvoteFaktagrunnlag
import no.nav.aap.behandlingsflyt.behandling.vilkår.kvote.OrdinærKvoteVilkår
import no.nav.aap.behandlingsflyt.behandling.vilkår.kvote.SykepengeerstatningKvoteFaktagrunnlag
import no.nav.aap.behandlingsflyt.behandling.vilkår.kvote.SykepengeerstatningKvoteVilkår
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.rettighetstype.RettighetstypeFaktagrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.rettighetstype.RettighetstypeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.ApplikasjonsVersjon
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

class RettighetstypeSteg(
    val rettighetstypeRepository: RettighetstypeRepository,
    val vilkårsresultatRepository: VilkårsresultatRepository,
    val kvoteService: KvoteService
) : BehandlingSteg {
    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        rettighetstypeRepository = repositoryProvider.provide(),
        vilkårsresultatRepository = repositoryProvider.provide(),
        kvoteService = KvoteService()
    )

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        when (kontekst.vurderingType) {
            VurderingType.FØRSTEGANGSBEHANDLING,
            VurderingType.REVURDERING,
            VurderingType.MIGRER_RETTIGHETSPERIODE -> {
                val behandlingId = kontekst.behandlingId

                val vilkårsresultat = vilkårsresultatRepository.hent(behandlingId)

                val kvoter = kvoteService.beregn()
                val kvotevurdering = vurderRettighetstypeOgKvoter(vilkårsresultat, kvoter)


                OrdinærKvoteVilkår(vilkårsresultat).vurder(OrdinærKvoteFaktagrunnlag(kvotevurdering, kvoter))

                SykepengeerstatningKvoteVilkår(vilkårsresultat).vurder(
                    SykepengeerstatningKvoteFaktagrunnlag(
                        kvotevurdering,
                        kvoter
                    )
                )

                val oppdatertRettighetstidslinje = vilkårsresultat.rettighetstypeTidslinje()

                val faktagrunnlag = RettighetstypeFaktagrunnlag(vilkårsresultat)
                rettighetstypeRepository.lagre(
                    behandlingId,
                    oppdatertRettighetstidslinje,
                    faktagrunnlag,
                    ApplikasjonsVersjon.versjon
                )


            }

            VurderingType.IKKE_RELEVANT,
            VurderingType.UTVID_VEDTAKSLENGDE,
            VurderingType.MELDEKORT,
            VurderingType.AUTOMATISK_BREV,
            VurderingType.EFFEKTUER_AKTIVITETSPLIKT,
            VurderingType.EFFEKTUER_AKTIVITETSPLIKT_11_9 -> {
                // Bruker persistert rettighetstype
            }
        }

        return Fullført
    }
    
    companion object : FlytSteg {
        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): BehandlingSteg {
            return RettighetstypeSteg(repositoryProvider, gatewayProvider)
        }

        override fun type(): StegType {
            return StegType.FASTSETT_RETTIGHETSTYPE
        }
    }
}