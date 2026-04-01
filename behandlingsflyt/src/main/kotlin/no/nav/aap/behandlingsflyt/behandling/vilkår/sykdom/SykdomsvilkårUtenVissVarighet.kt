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
import no.nav.aap.komponenter.tidslinje.StandardSammenslåere
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.orEmpty
import no.nav.aap.komponenter.type.Periode

class SykdomsvilkårUtenVissVarighet(vilkårsresultat: Vilkårsresultat) : Vilkårsvurderer<SykdomsFaktagrunnlag> {
    private val vilkår: Vilkår = vilkårsresultat.finnVilkår(Vilkårtype.SYKDOMSVILKÅRET)

    override fun vurder(grunnlag: SykdomsFaktagrunnlag) {
        val tidslinje = vurderVilkårUtenMutering(grunnlag)
        vilkår.leggTilVurderinger(tidslinje)
    }

    fun vurderOgSammenlign(
        grunnlag: SykdomsFaktagrunnlag,
        eksisterendeVilkårsresultat: Vilkårsresultat
    ): Tidslinje<SammenlignetSegment> {
        val nyTidslinje = vurderVilkårUtenMutering(grunnlag)
        val nySammenlignbarTidslinje =
            nyTidslinje.mapValue { SammenlignbarVurdering(it.utfall, it.innvilgelsesårsak, it.avslagsårsak) }
                .komprimer()

        val gammelTidslinje =
            eksisterendeVilkårsresultat.optionalVilkår(Vilkårtype.SYKDOMSVILKÅRET)?.tidslinje().orEmpty()
        val gammelSammenlignbarTidslinje =
            gammelTidslinje.mapValue { SammenlignbarVurdering(it.utfall, it.innvilgelsesårsak, it.avslagsårsak) }
                .komprimer()

        return nySammenlignbarTidslinje.outerJoin(gammelSammenlignbarTidslinje) { gammel, ny ->
            SammenlignetSegment(gammel, ny)
        }.komprimer()
    }

    fun vurderVilkårUtenMutering(
        grunnlag: SykdomsFaktagrunnlag
    ): Tidslinje<Vilkårsvurdering> {
        val yrkesskadeVurderingTidslinje = Tidslinje(
            Periode(grunnlag.kravDato, grunnlag.sisteDagMedMuligYtelse),
            grunnlag.yrkesskadevurdering
        )

        val sykdomsvurderingTidslinje = grunnlag.sykdomsvurderinger
            .sortedBy { it.opprettet }
            .map { vurdering ->
                Tidslinje(
                    Periode(
                        fom = vurdering.vurderingenGjelderFra,
                        tom = grunnlag.sisteDagMedMuligYtelse
                    ),
                    vurdering
                )
            }
            .fold(Tidslinje<Sykdomsvurdering>()) { t1, t2 ->
                t1.kombiner(t2, StandardSammenslåere.prioriterHøyreSideCrossJoin())
            }

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
            val nedsettelseYrkesskade = sykdomVurdering?.utledErNedsettelseMerEnnYrkesskadegrense()

            avslagsårsak = if (sykdomVurdering?.erSkadeSykdomEllerLyteVesentligdel == false) {
                Avslagsårsak.IKKE_SYKDOM_SKADE_LYTE_VESENTLIGDEL
            } else if (nedsettelseHalvparten == ErNedsettelseMinstHalvpartenValg.NEI
                && nedsettelseYrkesskade != ErNedsettelseMerEnnYrkesskadegrenseValg.JA
            ) {
                Avslagsårsak.IKKE_NOK_REDUSERT_ARBEIDSEVNE
            } else if (nedsettelseHalvparten == ErNedsettelseMinstHalvpartenValg.JA_FORBIGÅENDE_PROBLEMER) {
                Avslagsårsak.IKKE_SYKDOM_AV_VISS_VARIGHET
            } else {
                Avslagsårsak.MANGLENDE_DOKUMENTASJON
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

data class SammenlignetSegment(val gammel: SammenlignbarVurdering?, val ny: SammenlignbarVurdering?)

fun Tidslinje<SammenlignetSegment>.diff() = this.segmenter().filter { it.verdi.gammel != it.verdi.ny }
fun Tidslinje<SammenlignetSegment>.harDiff() = this.segmenter().none { it.verdi.gammel != it.verdi.ny }
data class SammenlignbarVurdering(
    val utfall: Utfall,
    val innvilgelsesårsak: Innvilgelsesårsak?,
    val avslagsårsak: Avslagsårsak?
)
