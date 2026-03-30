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
import org.slf4j.LoggerFactory

class SykdomsvilkårFraLansering(
    vilkårsresultat: Vilkårsresultat,
    private val sammenlignMedNyLogikk: Boolean = false
) : Vilkårsvurderer<SykdomsFaktagrunnlag> {
    private val vilkår: Vilkår = vilkårsresultat.finnVilkår(Vilkårtype.SYKDOMSVILKÅRET)
    private val log = LoggerFactory.getLogger(javaClass)

    override fun vurder(grunnlag: SykdomsFaktagrunnlag) {
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

        val gammelTidslinje =
            kombinerAlleTidslinjer(
                yrkesskadeVurderingTidslinje,
                sykdomsvurderingTidslinje,
                sykepengeerstatningTidslinje,
                bistandvurderingtidslinje
            )
                .mapValue { (segmentPeriode, yrkesskadeVurdering, sykdomVurdering, sykepengerVurdering, bistandVurdering) ->
                    opprettVilkårsvurderingGammelLogikk(
                        segmentPeriode,
                        grunnlag.sykepengeerstatningVilkår,
                        sykdomVurdering,
                        yrkesskadeVurdering,
                        sykepengerVurdering,
                        bistandVurdering,
                        grunnlag
                    )
                }


        val nyTidslinje = kombinerAlleTidslinjer(
            yrkesskadeVurderingTidslinje,
            sykdomsvurderingTidslinje,
            sykepengeerstatningTidslinje,
            bistandvurderingtidslinje
        )
            .mapValue { (segmentPeriode, yrkesskadeVurdering, sykdomVurdering, sykepengerVurdering, bistandVurdering) ->
                opprettVilkårsvurderingNyLogikk(
                    segmentPeriode,
                    grunnlag.sykepengeerstatningVilkår,
                    sykdomVurdering,
                    yrkesskadeVurdering,
                    sykepengerVurdering,
                    bistandVurdering,
                    grunnlag
                )
            }

        if (sammenlignMedNyLogikk) {
            val nySammenlignbarTidslinje =
                nyTidslinje.mapValue { Triple(it.utfall, it.avslagsårsak, it.innvilgelsesårsak) }.komprimer()
            val gammelSammenlignbarTidslinje =
                gammelTidslinje.mapValue { Triple(it.utfall, it.avslagsårsak, it.innvilgelsesårsak) }
                    .komprimer()

            nySammenlignbarTidslinje.outerJoin(gammelSammenlignbarTidslinje) { gammel, ny ->
                if (gammel != ny) {
                    throw IllegalStateException("Gammel og ny vilkårsvurdering av sykdom ga ulikt resultat")
                }
            }
        }

        vilkår.leggTilVurderinger(gammelTidslinje)
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
        ) { segmentPeriode, a, b, c ->
            LokaltSegment(
                segmentPeriode,
                yrkesskadeVurdering = a?.first,
                sykdomVurdering = a?.second,
                sykepengerVurdering = b,
                bistandsvurdering = c
            )
        }
    }

    internal data class LokaltSegment(
        val segmentPeriode: Periode,
        val yrkesskadeVurdering: Yrkesskadevurdering?,
        val sykdomVurdering: Sykdomsvurdering?,
        val sykepengerVurdering: SykepengerVurdering?,
        val bistandsvurdering: Bistandsvurdering?
    )
    
    private fun opprettVilkårsvurderingGammelLogikk(
        segmentPeriode: Periode,
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

        if (sykdomVurdering?.erOppfyltForYrkesskadeSettBortIfraÅrsakssammenheng(
                grunnlag.kravDato,
                segmentPeriode
            ) == true && yrkesskadeVurdering?.erÅrsakssammenheng == true
        ) {
            utfall = Utfall.OPPFYLT
            innvilgelsesårsak = Innvilgelsesårsak.YRKESSKADE_ÅRSAKSSAMMENHENG
        } else if (sykdomVurdering?.erOppfyltOrdinær(
                grunnlag.kravDato,
                segmentPeriode
            ) == true && bistandsvurdering?.erBehovForBistand() == true
        ) {
            utfall = Utfall.OPPFYLT
            innvilgelsesårsak = null
        } else if (sykepengeerstatningVilkår.isEmpty() && sykepengerVurdering?.harRettPå == true && sykdomVurdering?.erOppfyltOrdinærSettBortIfraVissVarighet() == true) {
            utfall = Utfall.OPPFYLT
            innvilgelsesårsak = Innvilgelsesårsak.SYKEPENGEERSTATNING
        } else {
            innvilgelsesårsak = null
            utfall = Utfall.IKKE_OPPFYLT
            avslagsårsak = if (sykdomVurdering?.erSkadeSykdomEllerLyteVesentligdel == false) {
                Avslagsårsak.IKKE_SYKDOM_SKADE_LYTE_VESENTLIGDEL
            } else if (sykdomVurdering?.erNedsettelseIArbeidsevneMerEnnHalvparten == false && sykdomVurdering.erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense != true) {
                Avslagsårsak.IKKE_NOK_REDUSERT_ARBEIDSEVNE
            } else if (sykdomVurdering?.erNedsettelseIArbeidsevneAvEnVissVarighet == false) {
                Avslagsårsak.IKKE_SYKDOM_AV_VISS_VARIGHET
            } else {
                Avslagsårsak.MANGLENDE_DOKUMENTASJON
            }

            log.info("Avslagsårsak $avslagsårsak. Er nedsettelse i arbeidsevne: ${sykdomVurdering?.erNedsettelseIArbeidsevneAvEnVissVarighet}")
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

    /**
     * Ny vilkårsvurdering som bruker enum-feltene og ignorerer kravdato-logikk.
     */
    private fun opprettVilkårsvurderingNyLogikk(
        segmentPeriode: Periode,
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

        if (sykdomVurdering?.erOppfyltForYrkesskadeSettBortIfraÅrsakssammenhengMedNyeFelter() == true
            && yrkesskadeVurdering?.erÅrsakssammenheng == true
        ) {
            utfall = Utfall.OPPFYLT
            innvilgelsesårsak = Innvilgelsesårsak.YRKESSKADE_ÅRSAKSSAMMENHENG
        } else if (sykdomVurdering?.erOppfyltOrdinærMedNyeFelter() == true
            && bistandsvurdering?.erBehovForBistand() == true
        ) {
            utfall = Utfall.OPPFYLT
            innvilgelsesårsak = null
        } else if (sykepengeerstatningVilkår.isEmpty() // Bakoverkompatibel - denne inngangen er egentlig ikke riktig
            && sykepengerVurdering?.harRettPå == true
            && sykdomVurdering?.skalVurderesForSykepengeerstatningMedNyeFelter() == true
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