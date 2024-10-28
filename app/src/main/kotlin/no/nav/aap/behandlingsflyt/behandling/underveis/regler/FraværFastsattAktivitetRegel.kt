package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.behandlingsflyt.behandling.underveis.regler.FraværFastsattAktivitetVurdering.Vilkårsvurdering.IKKE_RELEVANT_BRUDD
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.FraværFastsattAktivitetVurdering.Vilkårsvurdering.STANS_ANDRE_DAG
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.FraværFastsattAktivitetVurdering.Vilkårsvurdering.STANS_TI_DAGER_BRUKT_OPP
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.FraværFastsattAktivitetVurdering.Vilkårsvurdering.UNNTAK_INNTIL_EN_DAG
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.FraværFastsattAktivitetVurdering.Vilkårsvurdering.UNNTAK_STERKE_VELFERDSGRUNNER
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.FraværFastsattAktivitetVurdering.Vilkårsvurdering.UNNTAK_SYKDOM_ELLER_SKADE
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.AktivitetspliktRegistrering
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Brudd.Paragraf.PARAGRAF_11_8
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.BruddType.IKKE_MØTT_TIL_ANNEN_AKTIVITET
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.BruddType.IKKE_MØTT_TIL_BEHANDLING_ELLER_UTREDNING
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.BruddType.IKKE_MØTT_TIL_TILTAK
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Grunn.INGEN_GYLDIG_GRUNN
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Grunn.RIMELIG_GRUNN
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Grunn.STERKE_VELFERDSGRUNNER
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Grunn.SYKDOM_ELLER_SKADE
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.tidslinje.JoinStyle
import no.nav.aap.tidslinje.Segment
import no.nav.aap.tidslinje.Tidslinje
import org.slf4j.LoggerFactory

private const val KVOTE_KALENDERÅR = 10


/** Vurder om medlemmet kan sanksjoneres etter § 11-8 "Fravær fra fastsatt aktivitet".
 *
 * Implementasjon av:
 * - [Folketrygdloven § 11-8](https://lovdata.no/lov/1997-02-28-19/§11-8)
 * - [Forskriftens § 3](https://lovdata.no/forskrift/2017-12-13-2100/§3)
 */
class FraværFastsattAktivitetRegel : UnderveisRegel {
    private val log = LoggerFactory.getLogger(this::class.qualifiedName)!!

    companion object {
        private val relevanteBrudd = listOf(
            IKKE_MØTT_TIL_BEHANDLING_ELLER_UTREDNING,
            IKKE_MØTT_TIL_TILTAK,
            IKKE_MØTT_TIL_ANNEN_AKTIVITET,
        )
    }

    override fun vurder(input: UnderveisInput, resultat: Tidslinje<Vurdering>): Tidslinje<Vurdering> {
        /* TODO: § 11-8 stans til ... vilkårene igjen er oppfylt */

        val bruddtidslinjeGruppertPåMeldeperiode = resultat.kombiner(
            input.aktivitetspliktGrunnlag.tidslinje,
            JoinStyle.RIGHT_JOIN { periode, vurdering, brudd ->
                val meldeperiode = /* requireNotNull( */ vurdering?.verdi?.meldeperiode() /*, {
                    // Kan være fristende å bruke INNER_JOIN, men da forsvinner brudd fra tidslinja
                    "meldeperiode eksisterer ikke for $periode. må eksistere for å kunne gruppere brudd på meldeperiode"
                }) */
                Segment(periode, meldeperiode to brudd.verdi)
            }
        )
            .segmenter()
            .groupBy(
                { it.verdi.first },
                { Segment(it.periode, it.verdi.second) }
            )
            .mapNotNull { (meldeperiode, bruddIMeldeperioden) ->
                if (meldeperiode == null) {
                    log.warn("meldeperiode manger, ignorerer brudd")
                    null
                } else {
                    Segment(meldeperiode, Tidslinje(bruddIMeldeperioden.sortedBy { it.fom() }))
                }
            }
            .let { Tidslinje(it) }

        val bruddTidslinjeMedFørsteFraværIdentifisert: Tidslinje<AktivitetspliktSteg1> =
            bruddtidslinjeGruppertPåMeldeperiode
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
        meldeperioden: Tidslinje<AktivitetspliktRegistrering>,
        periode: Periode,
        dokument: AktivitetspliktRegistrering,
    ): Tidslinje<AktivitetspliktSteg1> {
        val førsteBrudd = meldeperioden.segmenter()
            .first { it.verdi.brudd.paragraf == PARAGRAF_11_8 }
            .verdi

        val erFørsteBruddMeldeperioden = førsteBrudd.metadata.id == dokument.metadata.id
        return if (erFørsteBruddMeldeperioden) {
            val førsteFravær = Periode(periode.fom, periode.fom)
            val periodene = listOf(førsteFravær) + periode.minus(førsteFravær)
            Tidslinje(
                periodene.map {
                    Segment(
                        it,
                        AktivitetspliktSteg1(
                            dokument = dokument,
                            førsteFraværIMeldeperioden = it == førsteFravær,
                        )
                    )
                }
            )
        } else {
            Tidslinje(
                periode,
                AktivitetspliktSteg1(
                    dokument = dokument,
                    førsteFraværIMeldeperioden = false,
                )
            )
        }
    }

    private fun vurderKalenderår(kalenderår: Tidslinje<AktivitetspliktSteg1>): Tidslinje<FraværFastsattAktivitetVurdering> {
        var nestePosisjonKalenderår = 1

        return kalenderår.flatMap { vurderingSegment ->
            val vurdering = vurderingSegment.verdi
            val dokument = vurdering.dokument
//                    Tidslinje(
//                        vurderingSegment.periode,
//                        FraværFastsattAktivitetVurdering(
//                            dokument = dokument,
//                            vilkårsvurdering = FEILREGISTRERT_BRUDD,
//                            skalStanses = false,
//                        )
//                    )
            if (dokument.brudd.bruddType !in relevanteBrudd) {
                return@flatMap Tidslinje(
                    vurderingSegment.periode,
                    FraværFastsattAktivitetVurdering(
                        dokument = dokument,
                        vilkårsvurdering = IKKE_RELEVANT_BRUDD,
                        skalStanses = false,
                    )
                )
            }

            if (vurdering.førsteFraværIMeldeperioden) {
                assert(vurderingSegment.periode.antallDager() == 1)
                return@flatMap Tidslinje(
                    vurderingSegment.periode,
                    FraværFastsattAktivitetVurdering(
                        dokument = dokument,
                        vilkårsvurdering = UNNTAK_INNTIL_EN_DAG,
                        skalStanses = false,
                    )
                )
            }

            when (dokument.grunn) {
                SYKDOM_ELLER_SKADE ->
                    Tidslinje(
                        vurderingSegment.periode,
                        FraværFastsattAktivitetVurdering(
                            dokument = dokument,
                            vilkårsvurdering = UNNTAK_SYKDOM_ELLER_SKADE,
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
                        val stansDag =
                            vurderingSegment.periode.fom.plusDays(stansInnenforPeriodeOffset.toLong())
                        val stansPeriode = Periode(stansDag, stansDag)
                        val utenfor = vurderingSegment.periode.minus(stansPeriode)
                        val perioder = (utenfor + stansPeriode).toList().sortedBy { it.fom }

                        return@flatMap Tidslinje(
                            perioder.map {
                                Segment(
                                    it,
                                    FraværFastsattAktivitetVurdering(
                                        dokument = dokument,
                                        vilkårsvurdering = if (it.fom >= stansDag) STANS_TI_DAGER_BRUKT_OPP else UNNTAK_STERKE_VELFERDSGRUNNER,
                                        skalStanses = dokument.brudd.paragraf == PARAGRAF_11_8,
                                    )
                                )
                            }
                        )
                    }

                    Tidslinje(
                        vurderingSegment.periode,
                        FraværFastsattAktivitetVurdering(
                            dokument = dokument,
                            vilkårsvurdering = UNNTAK_STERKE_VELFERDSGRUNNER,
                            skalStanses = false,
                        )
                    )
                }

                RIMELIG_GRUNN,
                INGEN_GYLDIG_GRUNN ->
                    Tidslinje(
                        vurderingSegment.periode,
                        FraværFastsattAktivitetVurdering(
                            dokument = dokument,
                            vilkårsvurdering = STANS_ANDRE_DAG,
                            skalStanses = dokument.brudd.paragraf == PARAGRAF_11_8,
                        )
                    )
            }
        }
    }


    class AktivitetspliktSteg1(
        val dokument: AktivitetspliktRegistrering,
        val førsteFraværIMeldeperioden: Boolean,
    )
}
