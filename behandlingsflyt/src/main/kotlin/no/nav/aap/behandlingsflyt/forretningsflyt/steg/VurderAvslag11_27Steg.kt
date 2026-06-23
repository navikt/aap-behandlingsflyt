package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.behandling.avslag11_27.Avslag11_27Repository
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
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
import no.nav.aap.lookup.repository.RepositoryProvider

class VurderAvslag11_27Steg private constructor(
    private val avslag11_27repository: Avslag11_27Repository,
    private val avklaringsbehovService: AvklaringsbehovService,
    private val tidligereVurderinger: TidligereVurderinger,
    private val vilkårsresultatRepository: VilkårsresultatRepository
) : BehandlingSteg {

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        avklaringsbehovService.oppdaterAvklaringsbehov(
            kontekst = kontekst,
            definisjon = Definisjon.VURDER_AVSLAG_11_27,
            vedtakBehøverVurdering = { vedtakBehøverVurdering(kontekst) },
            erTilstrekkeligVurdert = {
                avslag11_27repository.hentHvisEksisterer(kontekst.behandlingId) != null
            },
            tilbakestillGrunnlag = {
                avslag11_27repository.tilbakestillGrunnlag(
                    kontekst.behandlingId,
                    kontekst.forrigeBehandlingId
                )
            },
        )

        settVilkårsresultat(kontekst)

        return Fullført
    }

    private fun settVilkårsresultat(kontekst: FlytKontekstMedPerioder) {
        val grunnlag = avslag11_27repository.hentHvisEksisterer(kontekst.behandlingId) ?: return
        val nåværendeVurderinger = grunnlag.vurderinger.filter { it.vurdertIBehandling == kontekst.behandlingId }
        if (nåværendeVurderinger.isEmpty()) return

        val harAvslag = nåværendeVurderinger.all { it.skalAvslås1127 }

        val vilkårsresultat = vilkårsresultatRepository.hent(kontekst.behandlingId)
        val vilkår = vilkårsresultat.leggTilHvisIkkeEksisterer(Vilkårtype.AVSLAG_11_27).nullstillTidslinje()

        vilkår.leggTilVurdering(
            Vilkårsperiode(
                periode = kontekst.rettighetsperiode,
                vilkårsvurdering = Vilkårsvurdering(
                    utfall = if (harAvslag) Utfall.IKKE_OPPFYLT else Utfall.OPPFYLT,
                    manuellVurdering = true,
                    begrunnelse = if (harAvslag) "§ 11-27 avslag" else "§ 11-27 ikke avslag",
                    avslagsårsak = if (harAvslag) Avslagsårsak.ANNEN_FULL_YTELSE_11_27 else null,
                    faktagrunnlag = null,
                )
            )
        )
        vilkårsresultatRepository.lagre(kontekst.behandlingId, vilkårsresultat)
    }

    private fun vedtakBehøverVurdering(kontekst: FlytKontekstMedPerioder): Boolean {
        return when (kontekst.vurderingType) {
            VurderingType.FØRSTEGANGSBEHANDLING,
            VurderingType.REVURDERING ->
                (tidligereVurderinger.muligMedRettTilAAP(kontekst, type())
                        && Vurderingsbehov.VURDER_AVSLAG_11_27 in kontekst.vurderingsbehovRelevanteForSteg
                        && kontekst.vurderingsbehovRelevanteForSteg.isNotEmpty())

            VurderingType.MELDEKORT,
            VurderingType.AUTOMATISK_BREV,
            VurderingType.UTVID_VEDTAKSLENGDE,
            VurderingType.MIGRER_RETTIGHETSPERIODE,
            VurderingType.EFFEKTUER_AKTIVITETSPLIKT,
            VurderingType.EFFEKTUER_AKTIVITETSPLIKT_11_9,
            VurderingType.G_REGULERING,
            VurderingType.OVERGANG_UFORE_STANS,
            VurderingType.IKKE_RELEVANT ->
                false
        }
    }

    companion object : FlytSteg {
        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): BehandlingSteg {
            return VurderAvslag11_27Steg(
                avslag11_27repository = repositoryProvider.provide(),
                avklaringsbehovService = AvklaringsbehovService(repositoryProvider),
                tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider, gatewayProvider),
                vilkårsresultatRepository = repositoryProvider.provide()
            )
        }

        override fun type(): StegType {
            return StegType.VURDER_AVSLAG_11_27
        }
    }
}
