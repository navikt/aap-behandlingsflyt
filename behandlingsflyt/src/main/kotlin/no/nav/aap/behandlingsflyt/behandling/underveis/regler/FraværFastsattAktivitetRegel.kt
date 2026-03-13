package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.behandlingsflyt.behandling.underveis.regler.FraværFastsattAktivitetVurdering.Vilkårsvurdering.MER_ENN_EN_DAGS_FRAVÆR_I_MELDEPERIODE
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.FraværFastsattAktivitetVurdering.Vilkårsvurdering.MER_ENN_TI_DAGERS_FRAVÆR_I_KALENDERÅR
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.FraværFastsattAktivitetVurdering.Vilkårsvurdering.FRAVÆR_FØRSTE_DAG_I_MELDEPERIODE
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.FraværFastsattAktivitetVurdering.Vilkårsvurdering.FRAVÆR_STERK_VELFERDSGRUNN
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.FraværFastsattAktivitetVurdering.Vilkårsvurdering.FRAVÆR_SYKDOM_ELLER_SKADE
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.FraværIPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.FraværÅrsak
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.somTidslinje
import no.nav.aap.komponenter.type.Periode
import kotlin.collections.plus

private const val KVOTE_KALENDERÅR = 10
private const val KVOTE_BARN = 3
private const val KVOTE_DØDSFALL = 3


/** Vurder om medlemmet kan sanksjoneres etter § 11-8 "Fravær fra fastsatt aktivitet".
 *
 * Implementasjon av:
 * - [Folketrygdloven § 11-8](https://lovdata.no/lov/1997-02-28-19/§11-8)
 * - [Forskriftens § 3](https://lovdata.no/forskrift/2017-12-13-2100/§3)
 */
class FraværFastsattAktivitetRegel : UnderveisRegel {
    companion object {
        private val gyldigeÅrsaker = listOf(
            FraværÅrsak.OMSORG_FØRSTE_SKOLEDAG_TILVENNING_ELLER_ANNEN_OPPFØLGING_BARN,
            FraværÅrsak.OMSORG_PLEIE_I_HJEMMET_AV_NÆR_PÅRØRENDE,
            FraværÅrsak.OMSORG_DØDSFALL_I_FAMILIE_ELLER_VENNEKRETS,
            FraværÅrsak.OMSORG_ANNEN_STERK_GRUNN,

            FraværÅrsak.SYKDOM_ELLER_SKADE,
        )
    }

    private fun Tidslinje<FraværFastsattAktivitetVurdering>.sammenslåTilstøtendeVurderinger(): Tidslinje<FraværFastsattAktivitetVurdering> =
        segmenter()
            .fold(emptyList<Segment<FraværFastsattAktivitetVurdering>>()) { acc, current ->
                val last = acc.lastOrNull()

                if (last != null &&
                    last.verdi.utfall == current.verdi.utfall &&
                    last.periode.tom.plusDays(1) == current.periode.fom
                ) {
                    val sammenslåttPeriode = Periode(last.periode.fom, current.periode.tom)
                    acc.dropLast(1) + Segment(
                        sammenslåttPeriode,
                        last.verdi
                    )
                } else {
                    acc + current
                }
            }.let(::Tidslinje)


    override fun vurder(input: UnderveisInput, resultat: Tidslinje<Vurdering>): Tidslinje<Vurdering> {
        require(input.periodeForVurdering.inneholder(resultat.helePerioden())) {
            "kan ikke vurdere utenfor periode for vurdering fordi meldeperioden ikke er definert"
        }

        val fraværTidslinje: Tidslinje<FraværIPeriode> =
            input.meldekort
                .flatMap { it.fravær }
                .sortedBy { it.periode }
                .somTidslinje { it.periode }

        // Første dag med fravær uten gyldig årsak i meldeperioden teller ikke i årskvote
        // Deler opp på meldeperiode først for å finne første i meldeperioden
        // Dette brukes for å regne ut antall dager med reduksjon per kalenderår som utfall av vurderingen
        val fraværTidslinjeMedUnntakIdentifisert: Tidslinje<FraværMedUnntakVurdertForPeriode> =
            tidslinjeMedUnntakIdentifisert(input.meldeperioder, fraværTidslinje)

        val ferdigVurdert = fraværTidslinjeMedUnntakIdentifisert.splittOppKalenderår()
            .flatMap { kalenderårSegment ->
                vurderKalenderår(kalenderårSegment.verdi)
            }
            .sammenslåTilstøtendeVurderinger()
            .begrensetTil(input.periodeForVurdering) // TODO skal dette gjøres?

        return resultat.leggTilVurderinger(ferdigVurdert, Vurdering::leggTilAktivitetspliktVurdering)
    }

    private fun tidslinjeMedUnntakIdentifisert(
        meldeperioder: List<Periode>,
        fravær: Tidslinje<FraværIPeriode>,
    ): Tidslinje<FraværMedUnntakVurdertForPeriode> {
        return fravær.splittOppIPerioder(meldeperioder)
            .flatMap { meldeperiodenSegment ->
                vurderMeldeperiode(meldeperiodenSegment.verdi)
            }
    }

    /**
     * [meldeperioden] er en tidslinje som bare skal inneholde segment med periode innenfor en gitt meldeperiode.
     */
    private fun vurderMeldeperiode(meldeperioden: Tidslinje<FraværIPeriode>): Tidslinje<FraværMedUnntakVurdertForPeriode> {
        return meldeperioden.flatMap { fraværSegment ->
            vurderMeldeperiode(meldeperioden, fraværSegment.periode, fraværSegment.verdi)
        }
    }

    private fun vurderMeldeperiode(
        meldeperioden: Tidslinje<FraværIPeriode>,
        periode: Periode,
        fravær: FraværIPeriode,
    ): Tidslinje<FraværMedUnntakVurdertForPeriode> {
        val inntilEnDagUnntakUtenGyldigGrunn = meldeperioden.segmenter().singleOrNull {
            it.verdi.fraværÅrsak !in gyldigeÅrsaker
        }?.verdi

        val harInntilEnDagUnntak = inntilEnDagUnntakUtenGyldigGrunn == fravær
        return if (harInntilEnDagUnntak) {
            val førsteFravær = Periode(periode.fom, periode.fom)
            val periodene = listOf(førsteFravær) + periode.minus(førsteFravær)
            Tidslinje(
                periodene.map {
                    Segment(
                        it,
                        FraværMedUnntakVurdertForPeriode(
                            fravær = fravær,
                            // finnesEnDagFraværUtenGyldigÅrsak = true/false
                            erUnntak = it == førsteFravær,
                        )
                    )
                }
            )
        } else {
            Tidslinje(
                periode,
                // FraværIPeriodeMedUnntak
                FraværMedUnntakVurdertForPeriode(
                    fravær = fravær,
                    erUnntak = false,
                )
            )
        }
    }

    /**
     * [kalenderår] er en tidslinje som bare skal inneholde segment med periode innenfor et gitt år.
     */
    private fun vurderKalenderår(kalenderår: Tidslinje<FraværMedUnntakVurdertForPeriode>): Tidslinje<FraværFastsattAktivitetVurdering> {
        var kalenderårskvote = 0

        return kalenderår.flatMap { vurderingSegment ->
            val vurdering = vurderingSegment.verdi
            val fravær = vurdering.fravær

            when (fravær.fraværÅrsak) {
                FraværÅrsak.SYKDOM_ELLER_SKADE ->
                    Tidslinje(
                        vurderingSegment.periode,
                        FraværFastsattAktivitetVurdering(
//                            fravær = fravær,
                            vilkårsvurdering = FRAVÆR_SYKDOM_ELLER_SKADE,
                        )
                    )

                FraværÅrsak.OMSORG_FØRSTE_SKOLEDAG_TILVENNING_ELLER_ANNEN_OPPFØLGING_BARN, // TODO kvote
                FraværÅrsak.OMSORG_PLEIE_I_HJEMMET_AV_NÆR_PÅRØRENDE,
                FraværÅrsak.OMSORG_DØDSFALL_I_FAMILIE_ELLER_VENNEKRETS, // TODO kvote
                FraværÅrsak.OMSORG_ANNEN_STERK_GRUNN -> {
                    (0..<vurderingSegment.periode.antallDager()).map { periodeOffset ->
                        kalenderårskvote += 1
                        val dag = vurderingSegment.periode.fom.plusDays(periodeOffset.toLong())
                        val periode = Periode(dag, dag)
                        Segment(
                            periode,
                            FraværFastsattAktivitetVurdering(
//                                fravær = fravær,
                                vilkårsvurdering =
                                    if (kalenderårskvote > KVOTE_KALENDERÅR) MER_ENN_TI_DAGERS_FRAVÆR_I_KALENDERÅR
                                    else FRAVÆR_STERK_VELFERDSGRUNN,
                            )
                        )
                    }
                        .let { Tidslinje(it) }
                        .komprimer()
                }

                FraværÅrsak.ANNET ->
                    Tidslinje(
                        vurderingSegment.periode,
                        FraværFastsattAktivitetVurdering(
//                            fravær = fravær,
                            vilkårsvurdering =
                                if (vurdering.erUnntak) FRAVÆR_FØRSTE_DAG_I_MELDEPERIODE
                                else MER_ENN_EN_DAGS_FRAVÆR_I_MELDEPERIODE,
                        )
                    )
            }
        }
    }


    class FraværMedUnntakVurdertForPeriode(
        val fravær: FraværIPeriode,
        val erUnntak: Boolean,
    )
}


