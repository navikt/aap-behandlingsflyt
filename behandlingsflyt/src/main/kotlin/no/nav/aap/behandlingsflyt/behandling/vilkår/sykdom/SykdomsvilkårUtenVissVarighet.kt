package no.nav.aap.behandlingsflyt.behandling.vilkår.sykdom

import no.nav.aap.behandlingsflyt.behandling.vilkår.Vilkårsvurderer
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Innvilgelsesårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkår
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.Bistandsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.ErNedsettelseMerEnnYrkesskadegrenseValg
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.ErNedsettelseMinstHalvpartenValg
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Sykdomsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykepengerVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Yrkesskadevurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.somSykdomsvurderingTidslinje
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.orEmpty
import no.nav.aap.komponenter.type.Periode

class SykdomsvilkårUtenVissVarighet(vilkårsresultat: Vilkårsresultat) : Vilkårsvurderer<SykdomsFaktagrunnlag> {
    private val vilkår: Vilkår = vilkårsresultat.finnVilkår(Vilkårtype.SYKDOMSVILKÅRET)

    override fun vurder(grunnlag: SykdomsFaktagrunnlag) {
        val tidslinje = vurderVilkårUtenMutering(grunnlag)
        vilkår.leggTilVurderinger(tidslinje)
    }

    fun vurderVilkårUtenMutering(
        grunnlag: SykdomsFaktagrunnlag
    ): Tidslinje<Vilkårsvurdering> {
        val yrkesskadeVurderingTidslinje = Tidslinje(
            Periode(grunnlag.kravDato, grunnlag.sisteDagMedMuligYtelse),
            grunnlag.yrkesskadevurdering
        )

        val sykdomsvurderingTidslinje = grunnlag.sykdomsvurderinger.somSykdomsvurderingTidslinje(grunnlag.sisteDagMedMuligYtelse)

        val bistandvurderingtidslinje =
            grunnlag.bistandvurderingFaktagrunnlag
                ?.somBistandsvurderingstidslinje(grunnlag.sisteDagMedMuligYtelse)
                .orEmpty()

        val sykepengeerstatningTidslinje = grunnlag.sykepengerErstatningFaktagrunnlag?.somTidslinje(
            kravDato = grunnlag.kravDato,
            sisteMuligDagMedYtelse = grunnlag.sisteDagMedMuligYtelse
        ).orEmpty()


        return kombinerAlleTidslinjer(
            yrkesskadeVurderingTidslinje,
            sykdomsvurderingTidslinje,
            sykepengeerstatningTidslinje,
            bistandvurderingtidslinje
        )
            .mapValue { (yrkesskadeVurdering, sykdomVurdering, sykepengerVurdering, bistandVurdering) ->
                opprettVilkårsvurdering(
                    grunnlag.sykepengeerstatningVilkår,
                    sykdomVurdering,
                    yrkesskadeVurdering,
                    sykepengerVurdering,
                    bistandVurdering,
                    grunnlag
                )
            }

    }

    private fun kombinerAlleTidslinjer(
        yrkesskadeVurderingTidslinje: Tidslinje<Yrkesskadevurdering?>,
        sykdomsvurderingTidslinje: Tidslinje<Sykdomsvurdering>,
        sykepengerTidslinje: Tidslinje<SykepengerVurdering>,
        bistandvurderingTidslinje: Tidslinje<Bistandsvurdering>,
    ): Tidslinje<LokaltSegment> {
        val zip2 = Tidslinje.zip2(
            yrkesskadeVurderingTidslinje,
            sykdomsvurderingTidslinje,
        )

        return Tidslinje.map3(
            zip2,
            sykepengerTidslinje,
            bistandvurderingTidslinje,
        ) { a, b, c ->
            LokaltSegment(
                yrkesskadeVurdering = a?.first,
                sykdomVurdering = a?.second,
                sykepengerVurdering = b,
                bistandsvurdering = c
            )
        }
    }

    internal data class LokaltSegment(
        val yrkesskadeVurdering: Yrkesskadevurdering?,
        val sykdomVurdering: Sykdomsvurdering?,
        val sykepengerVurdering: SykepengerVurdering?,
        val bistandsvurdering: Bistandsvurdering?
    )


    private fun opprettVilkårsvurdering(
        sykepengeerstatningVilkår: Tidslinje<Vilkårsvurdering>,
        sykdomVurdering: Sykdomsvurdering?,
        yrkesskadeVurdering: Yrkesskadevurdering?,
        sykepengerVurdering: SykepengerVurdering?,
        bistandsvurdering: Bistandsvurdering?,
        grunnlag: SykdomsFaktagrunnlag
    ): Vilkårsvurdering {
        var utfall: Utfall
        var avslagsårsak: Avslagsårsak? = null
        var innvilgelsesårsak: Innvilgelsesårsak?

        if (sykdomVurdering?.erOppfyltForOrdinærEllerYrkesskadeSettBortIfraÅrsakssammenhengMedUtlededeFelter() == true
            && yrkesskadeVurdering?.erÅrsakssammenheng == true
        ) {
            utfall = Utfall.OPPFYLT
            innvilgelsesårsak = Innvilgelsesårsak.YRKESSKADE_ÅRSAKSSAMMENHENG
        } else if (sykdomVurdering?.erOppfyltOrdinærMedUtlededeFelter() == true
            && bistandsvurdering?.erBehovForBistand() == true
        ) {
            utfall = Utfall.OPPFYLT
            innvilgelsesårsak = null
        } else if (sykepengeerstatningVilkår.isEmpty() // Bakoverkompatibel - denne inngangen er egentlig ikke riktig
            && sykepengerVurdering?.harRettPå == true
            && sykdomVurdering?.skalVurderesForSykepengeerstatningMedUtlededeFelter() == true
        ) {
            utfall = Utfall.OPPFYLT
            innvilgelsesårsak = Innvilgelsesårsak.SYKEPENGEERSTATNING
        } else {
            innvilgelsesårsak = null
            utfall = Utfall.IKKE_OPPFYLT

            val nedsettelseHalvparten = sykdomVurdering?.utledErNedsettelseMinstHalvparten()

            avslagsårsak = when {
                sykdomVurdering?.harSkadeSykdomEllerLyte == false ->
                    Avslagsårsak.IKKE_SYKDOM_SKADE_LYTE

                sykdomVurdering?.erSkadeSykdomEllerLyteVesentligdel == false ->
                    Avslagsårsak.IKKE_SYKDOM_SKADE_LYTE_VESENTLIGDEL

                nedsettelseHalvparten == ErNedsettelseMinstHalvpartenValg.JA_FORBIGÅENDE_PROBLEMER ->
                    Avslagsårsak.IKKE_SYKDOM_AV_VISS_VARIGHET

                else ->
                    Avslagsårsak.IKKE_NOK_REDUSERT_ARBEIDSEVNE
            }
        }

        return Vilkårsvurdering(
            Vilkårsperiode(
                periode = Periode(grunnlag.kravDato, grunnlag.sisteDagMedMuligYtelse),
                utfall = utfall,
                begrunnelse = null,
                innvilgelsesårsak = innvilgelsesårsak,
                avslagsårsak = avslagsårsak,
                faktagrunnlag = grunnlag,
            )
        )
    }

}