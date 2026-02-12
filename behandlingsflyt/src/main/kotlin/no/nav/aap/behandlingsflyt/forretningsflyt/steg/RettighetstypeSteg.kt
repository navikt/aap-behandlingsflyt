package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.rettighetstype.KvoteOk
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
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

class RettighetstypeSteg(
    val rettighetstypeRepository: RettighetstypeRepository,
    val vilkårsresultatRepository: VilkårsresultatRepository,
    val kvoteService: KvoteService,
    val unleashGateway: UnleashGateway
) : BehandlingSteg {
    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        rettighetstypeRepository = repositoryProvider.provide(),
        vilkårsresultatRepository = repositoryProvider.provide(),
        kvoteService = KvoteService(),
        unleashGateway = gatewayProvider.provide()
    )

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        if (unleashGateway.isDisabled(BehandlingsflytFeature.RettighetstypeSteg)) {
            return Fullført
        }
        
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