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
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.ArbeidsevneNedsattValg
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.ErNedsettelseMinstHalvpartenValg
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Sykdomsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykepengerVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Yrkesskadevurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.somSykdomsvurderingTidslinje
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.orEmpty
import no.nav.aap.komponenter.type.Periode
import org.slf4j.LoggerFactory

class SykdomsvilkårUtenVissVarighet(vilkårsresultat: Vilkårsresultat) : Vilkårsvurderer<SykdomsFaktagrunnlag> {
    private val vilkår: Vilkår = vilkårsresultat.finnVilkår(Vilkårtype.SYKDOMSVILKÅRET)
    private val log = LoggerFactory.getLogger(javaClass)

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

        val sykdomsvurderingTidslinje =
            grunnlag.sykdomsvurderinger.somSykdomsvurderingTidslinje(grunnlag.sisteDagMedMuligYtelse)

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

        validerNyeHjelpemetoder(sykdomVurdering)

        if (sykdomVurdering?.erOppfyltForOrdinærEllerYrkesskadeSettBortIfraÅrsakssammenhengMedUtlededeFelter() == true
            && yrkesskadeVurdering?.erÅrsakssammenheng == true
        ) {
            utfall = Utfall.OPPFYLT
            innvilgelsesårsak = Innvilgelsesårsak.YRKESSKADE_ÅRSAKSSAMMENHENG
        } else if (sykdomVurdering?.erOppfyltOrdinærMedUtlededeFelterGammel() == true
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

            avslagsårsak = when {
                sykdomVurdering?.harSkadeSykdomEllerLyte == false ->
                    Avslagsårsak.IKKE_SYKDOM_SKADE_LYTE

                sykdomVurdering?.erSkadeSykdomEllerLyteVesentligdel == false ->
                    Avslagsårsak.IKKE_SYKDOM_SKADE_LYTE_VESENTLIGDEL

                utledHvorvidtVissVarighetErAvslagsårsak(sykdomVurdering) ->
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

    private fun validerNyeHjelpemetoder(sykdomsvurdering: Sykdomsvurdering?) {
        if (sykdomsvurdering == null) {
            return
        }

        if (sykdomsvurdering.erOppfyltForOrdinærEllerYrkesskadeSettBortIfraÅrsakssammenhengMedUtlededeFelter() != sykdomsvurdering.erOppfyltForOrdinærEllerYrkesskadeSettBortIfraÅrsakssammenheng()) {
            log.error("Ulikt resultat for erOppfyltForOrdinærEllerYrkesskadeSettBortIfraÅrsakssammenheng")
        }
        if (sykdomsvurdering.erOppfyltOrdinærMedUtlededeFelterGammel() != sykdomsvurdering.erOppfyltOrdinærMedUtlededeFelter()) {
            log.error("Ulikt resultat for erOppfyltOrdinærMedUtlededeFelter")
        }
        if (sykdomsvurdering.skalVurderesForSykepengeerstatningMedUtlededeFelter() != sykdomsvurdering.skalVurderesForSykepengeerstatning()) {
            log.error("Ulikt resultat for skalVurderesForSykepengeerstatning")
        }
    }

    private fun utledHvorvidtVissVarighetErAvslagsårsak(sykdomsvurdering: Sykdomsvurdering?): Boolean {
        val gammelSjekk =
            sykdomsvurdering?.utledErNedsettelseMinstHalvparten() == ErNedsettelseMinstHalvpartenValg.JA_FORBIGÅENDE_PROBLEMER
        val nySjekk = sykdomsvurdering?.utledHarNedsattArbeidsevne() == ArbeidsevneNedsattValg.JA_FORBIGÅENDE_PROBLEMER

        if (gammelSjekk != nySjekk) {
            log.error("Ulikt resultat for viss varighet som avslagsårsak")
        }
        return gammelSjekk
    }
}