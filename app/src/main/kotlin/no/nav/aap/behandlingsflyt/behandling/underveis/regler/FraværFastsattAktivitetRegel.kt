package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import java.time.Period
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.FraværFastsattAktivitetVurdering.StansUtfall.IKKE_RELEVANT
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.FraværFastsattAktivitetVurdering.StansUtfall.INNTIL_EN_DAG_UNNTAK
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.FraværFastsattAktivitetVurdering.StansUtfall.STANS_ANDRE_DAG
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.FraværFastsattAktivitetVurdering.StansUtfall.STANS_TI_DAGER_BRUKT_OPP
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.FraværFastsattAktivitetVurdering.StansUtfall.STERKE_VELFERDSGRUNNER_UNNTAK
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.FraværFastsattAktivitetVurdering.StansUtfall.SYKDOM_ELLER_SKADE_UNNTAK
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.BruddAktivitetsplikt
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.BruddAktivitetsplikt.Paragraf.PARAGRAF_11_8
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.tidslinje.JoinStyle
import no.nav.aap.tidslinje.Segment
import no.nav.aap.tidslinje.Tidslinje

private const val KVOTE_KALENDERÅR = 10

/**
 * Vurder om medlemmet oppfyller aktivitetsplikten. Implementasjon av:
 * - [Folketrygdloven § 11-7](https://lovdata.no/lov/1997-02-28-19/§11-7)
 * - [Folketrygdloven § 11-8](https://lovdata.no/lov/1997-02-28-19/§11-8)
 * - [Forskriftens § 3](https://lovdata.no/forskrift/2017-12-13-2100/§3)
 */
class FraværFastsattAktivitetRegel : UnderveisRegel {
    override fun vurder(input: UnderveisInput, resultat: Tidslinje<Vurdering>): Tidslinje<Vurdering> {
        /* TODO § 11-7 */
        /* TODO: § 11-8 stans til ... vilkårene igjen er oppfylt */

        /* TODO: Dette kommer til å kræsje om det er overlappende brudd. */
        val bruddTidslinje = Tidslinje(
            input.bruddAktivitetsplikt
                .sortedBy { it.periode.fom }
                .map { Segment(it.periode, it) }
        )

        val steg1: Tidslinje<BruddVurderingSteg1> = bruddTidslinje.splittOpp(input.rettighetsperiode, Period.ofDays(14))
            .flatMap { meldeperiodenSegment ->
                val meldeperioden = meldeperiodenSegment.verdi

                return@flatMap meldeperioden.flatMap { bruddSegment ->
                    val brudd = bruddSegment.verdi
                    val førsteBrudd = meldeperioden.segmenter().first { it.verdi.paragraf == PARAGRAF_11_8 }.verdi
                    val erFørsteBruddMeldeperioden = førsteBrudd.id == brudd.id
                    if (erFørsteBruddMeldeperioden)
                        Tidslinje(
                            listOfNotNull(
                                Segment(
                                    bruddSegment.periode.deFørsteDagene(1),
                                    BruddVurderingSteg1(
                                        brudd = brudd,
                                        førsteFraværIMeldeperioden = true,
                                        tellerMot10DagersKvote = false,
                                    )
                                ),
                                if (bruddSegment.periode.antallDager() > 1)
                                    Segment(
                                        bruddSegment.periode.bortsettFraDeFørsteDagene(1),
                                        BruddVurderingSteg1(
                                            brudd = brudd,
                                            førsteFraværIMeldeperioden = false,
                                            tellerMot10DagersKvote = true,
                                        )
                                    )
                                else null
                            )
                        )
                    else
                        Tidslinje(
                            bruddSegment.periode,
                            BruddVurderingSteg1(
                                brudd = brudd,
                                førsteFraværIMeldeperioden = false,
                                tellerMot10DagersKvote = true,
                            )
                        )
                }
            }

        val steg2 = steg1.splittOppKalenderår()
            .flatMap { kalenderårSegment ->
                val kalenderår = kalenderårSegment.verdi
                var nestePosisjonKalenderår = 1

                kalenderår.flatMap kalenderår@{ vurderingSegment ->
                    val vurdering = vurderingSegment.verdi
                    val brudd = vurdering.brudd

                    if (!brudd.erFraværFraFastsattAktivitet) {
                        return@kalenderår Tidslinje(
                            vurderingSegment.periode,
                            FraværFastsattAktivitetVurdering(
                                brudd = brudd,
                                muligUtfall = IKKE_RELEVANT,
                                skalStanses = false,
                            )
                        )
                    }

                    var bruddetsPosisjonKalenderår: Int? = null
                    var kvoteBruktOppDennePerioden = false
                    if (vurdering.tellerMot10DagersKvote) {
                        bruddetsPosisjonKalenderår = nestePosisjonKalenderår
                        nestePosisjonKalenderår += vurderingSegment.periode.antallDager()
                        kvoteBruktOppDennePerioden =
                            (KVOTE_KALENDERÅR + 1) in (bruddetsPosisjonKalenderår..<nestePosisjonKalenderår)
                    }

                    if (vurdering.førsteFraværIMeldeperioden) {
                        assert(vurderingSegment.periode.antallDager() == 1)
                        return@kalenderår Tidslinje(
                            vurderingSegment.periode,
                            FraværFastsattAktivitetVurdering(
                                brudd = brudd,
                                muligUtfall = INNTIL_EN_DAG_UNNTAK,
                                skalStanses = false,
                            )
                        )
                    }

                    if (kvoteBruktOppDennePerioden) {
                        val stansInnenforPeriodeOffset = (KVOTE_KALENDERÅR + 1) - bruddetsPosisjonKalenderår!!
                        val stansDag = vurderingSegment.periode.fom.plusDays(stansInnenforPeriodeOffset.toLong())
                        val stansPeriode = Periode(stansDag, stansDag)
                        val utenfor = vurderingSegment.periode.minus(stansPeriode)
                        val perioder = (utenfor + stansPeriode).toList().sortedBy { it.fom }

                        return@kalenderår Tidslinje(
                            perioder.map {
                                Segment(
                                    it,
                                    FraværFastsattAktivitetVurdering(
                                        brudd = brudd,
                                        muligUtfall = if (it.fom >= stansDag) STANS_TI_DAGER_BRUKT_OPP else STERKE_VELFERDSGRUNNER_UNNTAK,
                                        skalStanses = brudd.paragraf == PARAGRAF_11_8,
                                    )
                                )
                            }
                        )
                    }

                    when (brudd.grunn) {
                        BruddAktivitetsplikt.Grunn.SYKDOM_ELLER_SKADE ->
                            Tidslinje(
                                vurderingSegment.periode,
                                FraværFastsattAktivitetVurdering(
                                    brudd = brudd,
                                    muligUtfall = SYKDOM_ELLER_SKADE_UNNTAK,
                                    skalStanses = false,
                                )
                            )

                        BruddAktivitetsplikt.Grunn.STERKE_VELFERDSGRUNNER ->
                            Tidslinje(
                                vurderingSegment.periode,
                                FraværFastsattAktivitetVurdering(
                                    brudd = brudd,
                                    muligUtfall = STERKE_VELFERDSGRUNNER_UNNTAK,
                                    skalStanses = false,
                                )
                            )

                        BruddAktivitetsplikt.Grunn.RIMELIG_GRUNN,
                        BruddAktivitetsplikt.Grunn.INGEN_GYLDIG_GRUNN ->
                            Tidslinje(
                                vurderingSegment.periode,
                                FraværFastsattAktivitetVurdering(
                                    brudd = brudd,
                                    muligUtfall = STANS_ANDRE_DAG,
                                    skalStanses = brudd.paragraf == PARAGRAF_11_8,
                                )
                            )
                    }
                }
            }


        return steg2.kombiner(
            resultat,
            JoinStyle.OUTER_JOIN
            { periode, bruddSegment, vurderingSegment ->
                if (bruddSegment == null) return@OUTER_JOIN vurderingSegment
                val vurdering = (vurderingSegment?.verdi ?: Vurdering())
                    .leggTilAktivitetspliktVurdering(bruddSegment.verdi)
                Segment(periode, vurdering)
            },
        )
    }

    class BruddVurderingSteg1(
        val brudd: BruddAktivitetsplikt,
        val førsteFraværIMeldeperioden: Boolean,
        val tellerMot10DagersKvote: Boolean,
    )
}

fun Periode.deFørsteDagene(dager: Int): Periode {
    return Periode(this.fom, this.fom.plusDays(dager.toLong() - 1))
}

fun Periode.bortsettFraDeFørsteDagene(dager: Int): Periode {
    return Periode(this.fom.plusDays(dager.toLong()), this.tom)
}
