package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import java.time.Period
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.FraværFastsattAktivitetVurdering.StansUtfall.IKKE_RELEVANT
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.FraværFastsattAktivitetVurdering.StansUtfall.STANS_ANDRE_DAG
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.FraværFastsattAktivitetVurdering.StansUtfall.STANS_TI_DAGER_BRUKT_OPP
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.FraværFastsattAktivitetVurdering.StansUtfall.UNNTAK_INNTIL_EN_DAG
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.FraværFastsattAktivitetVurdering.StansUtfall.UNNTAK_STERKE_VELFERDSGRUNNER
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.FraværFastsattAktivitetVurdering.StansUtfall.UNNTAK_SYKDOM_ELLER_SKADE
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.BruddAktivitetsplikt
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.BruddAktivitetsplikt.Grunn.INGEN_GYLDIG_GRUNN
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.BruddAktivitetsplikt.Grunn.RIMELIG_GRUNN
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.BruddAktivitetsplikt.Grunn.STERKE_VELFERDSGRUNNER
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.BruddAktivitetsplikt.Grunn.SYKDOM_ELLER_SKADE
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.BruddAktivitetsplikt.Paragraf.PARAGRAF_11_8
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.tidslinje.JoinStyle
import no.nav.aap.tidslinje.Segment
import no.nav.aap.tidslinje.Tidslinje

private const val KVOTE_KALENDERÅR = 10

/** Vurder om medlemmet kan sanksjoneres etter § 11-8 "Fravær fra fastsatt aktivitet".
 *
 * Implementasjon av:
 * - [Folketrygdloven § 11-8](https://lovdata.no/lov/1997-02-28-19/§11-8)
 * - [Forskriftens § 3](https://lovdata.no/forskrift/2017-12-13-2100/§3)
 */
class FraværFastsattAktivitetRegel : UnderveisRegel {
    override fun vurder(input: UnderveisInput, resultat: Tidslinje<Vurdering>): Tidslinje<Vurdering> {
        /* TODO: § 11-8 stans til ... vilkårene igjen er oppfylt */

        val bruddTidslinjeMedFørsteFraværIdentifisert: Tidslinje<BruddVurderingSteg1> =
            input.bruddAktivitetsplikt
                .splittOpp(input.rettighetsperiode, Period.ofDays(14))
                .flatMap { meldeperiodenSegment ->
                    val meldeperioden = meldeperiodenSegment.verdi
                    meldeperioden.flatMap { bruddSegment ->
                        vurderMeldeperiode(meldeperioden, bruddSegment.periode, bruddSegment.verdi)
                    }
                }

        val ferdigVurdert = bruddTidslinjeMedFørsteFraværIdentifisert.splittOppKalenderår()
            .flatMap { kalenderårSegment ->
                vurderKalenderår(kalenderårSegment.verdi)
            }


        return ferdigVurdert.kombiner(
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

    private fun vurderMeldeperiode(
        meldeperioden: Tidslinje<BruddAktivitetsplikt>,
        periode: Periode,
        brudd: BruddAktivitetsplikt,
    ): Tidslinje<BruddVurderingSteg1> {
        val førsteBrudd = meldeperioden.segmenter().first { it.verdi.paragraf == PARAGRAF_11_8 }.verdi
        val erFørsteBruddMeldeperioden = førsteBrudd.id == brudd.id
        return if (erFørsteBruddMeldeperioden) {
            val førsteFravær = Periode(periode.fom, periode.fom)
            val periodene = listOf(førsteFravær) + periode.minus(førsteFravær)
            Tidslinje(
                periodene.map {
                    Segment(
                        it,
                        BruddVurderingSteg1(
                            brudd = brudd,
                            førsteFraværIMeldeperioden = it == førsteFravær,
                        )
                    )
                }
            )
        } else {
            Tidslinje(
                periode,
                BruddVurderingSteg1(
                    brudd = brudd,
                    førsteFraværIMeldeperioden = false,
                )
            )
        }
    }

    private fun vurderKalenderår(kalenderår: Tidslinje<BruddVurderingSteg1>): Tidslinje<FraværFastsattAktivitetVurdering> {
        var nestePosisjonKalenderår = 1

        return kalenderår.flatMap { vurderingSegment ->
            val vurdering = vurderingSegment.verdi
            val brudd = vurdering.brudd

            if (!brudd.erFraværFraFastsattAktivitet) {
                return@flatMap Tidslinje(
                    vurderingSegment.periode,
                    FraværFastsattAktivitetVurdering(
                        brudd = brudd,
                        utfall = IKKE_RELEVANT,
                        skalStanses = false,
                    )
                )
            }


            if (vurdering.førsteFraværIMeldeperioden) {
                assert(vurderingSegment.periode.antallDager() == 1)
                return@flatMap Tidslinje(
                    vurderingSegment.periode,
                    FraværFastsattAktivitetVurdering(
                        brudd = brudd,
                        utfall = UNNTAK_INNTIL_EN_DAG,
                        skalStanses = false,
                    )
                )
            }

            when (brudd.grunn) {
                SYKDOM_ELLER_SKADE ->
                    Tidslinje(
                        vurderingSegment.periode,
                        FraværFastsattAktivitetVurdering(
                            brudd = brudd,
                            utfall = UNNTAK_SYKDOM_ELLER_SKADE,
                            skalStanses = false,
                        )
                    )

                STERKE_VELFERDSGRUNNER -> {
                    val bruddetsPosisjonKalenderår = nestePosisjonKalenderår
                    nestePosisjonKalenderår += vurderingSegment.periode.antallDager()
                    val kvoteBruktOppDennePerioden =
                        (KVOTE_KALENDERÅR + 1) in (bruddetsPosisjonKalenderår..<nestePosisjonKalenderår)

                    if (kvoteBruktOppDennePerioden) {
                        val stansInnenforPeriodeOffset = (KVOTE_KALENDERÅR + 1) - bruddetsPosisjonKalenderår
                        val stansDag = vurderingSegment.periode.fom.plusDays(stansInnenforPeriodeOffset.toLong())
                        val stansPeriode = Periode(stansDag, stansDag)
                        val utenfor = vurderingSegment.periode.minus(stansPeriode)
                        val perioder = (utenfor + stansPeriode).toList().sortedBy { it.fom }

                        return@flatMap Tidslinje(
                            perioder.map {
                                Segment(
                                    it,
                                    FraværFastsattAktivitetVurdering(
                                        brudd = brudd,
                                        utfall = if (it.fom >= stansDag) STANS_TI_DAGER_BRUKT_OPP else UNNTAK_STERKE_VELFERDSGRUNNER,
                                        skalStanses = brudd.paragraf == PARAGRAF_11_8,
                                    )
                                )
                            }
                        )
                    }

                    Tidslinje(
                        vurderingSegment.periode,
                        FraværFastsattAktivitetVurdering(
                            brudd = brudd,
                            utfall = UNNTAK_STERKE_VELFERDSGRUNNER,
                            skalStanses = false,
                        )
                    )
                }

                RIMELIG_GRUNN,
                INGEN_GYLDIG_GRUNN ->
                    Tidslinje(
                        vurderingSegment.periode,
                        FraværFastsattAktivitetVurdering(
                            brudd = brudd,
                            utfall = STANS_ANDRE_DAG,
                            skalStanses = brudd.paragraf == PARAGRAF_11_8,
                        )
                    )
            }
        }
    }


    class BruddVurderingSteg1(
        val brudd: BruddAktivitetsplikt,
        val førsteFraværIMeldeperioden: Boolean,
    )
}
