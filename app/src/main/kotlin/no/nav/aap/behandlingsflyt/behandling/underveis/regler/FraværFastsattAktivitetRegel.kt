package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.behandlingsflyt.behandling.underveis.regler.FraværFastsattAktivitetVurdering.Vilkårsvurdering.STANS_ANDRE_DAG
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.FraværFastsattAktivitetVurdering.Vilkårsvurdering.STANS_TI_DAGER_BRUKT_OPP
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.FraværFastsattAktivitetVurdering.Vilkårsvurdering.UNNTAK_INNTIL_EN_DAG
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.FraværFastsattAktivitetVurdering.Vilkårsvurdering.UNNTAK_STERKE_VELFERDSGRUNNER
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.FraværFastsattAktivitetVurdering.Vilkårsvurdering.UNNTAK_SYKDOM_ELLER_SKADE
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.UtledMeldeperiodeRegel.Companion.groupByMeldeperiode
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
import no.nav.aap.tidslinje.Segment
import no.nav.aap.tidslinje.StandardSammenslåere
import no.nav.aap.tidslinje.Tidslinje

private const val KVOTE_KALENDERÅR = 10


/** Vurder om medlemmet kan sanksjoneres etter § 11-8 "Fravær fra fastsatt aktivitet".
 *
 * Implementasjon av:
 * - [Folketrygdloven § 11-8](https://lovdata.no/lov/1997-02-28-19/§11-8)
 * - [Forskriftens § 3](https://lovdata.no/forskrift/2017-12-13-2100/§3)
 */
class FraværFastsattAktivitetRegel : UnderveisRegel {
    companion object {
        private val relevanteBrudd = listOf(
            IKKE_MØTT_TIL_BEHANDLING_ELLER_UTREDNING,
            IKKE_MØTT_TIL_TILTAK,
            IKKE_MØTT_TIL_ANNEN_AKTIVITET,
        )

        private val gyldigeGrunner = listOf(
            STERKE_VELFERDSGRUNNER, SYKDOM_ELLER_SKADE
        )
    }

    override fun vurder(input: UnderveisInput, resultat: Tidslinje<Vurdering>): Tidslinje<Vurdering> {
        val høyerePrioritertVurdering = resultat.filter { it.verdi.aktivitetspliktVurdering != null }
        val tidslinje = input.aktivitetspliktGrunnlag.tidslinje(PARAGRAF_11_8)
            .kombiner(høyerePrioritertVurdering, StandardSammenslåere.minus())


        require(tidslinje.segmenter().all { it.verdi.brudd.bruddType in relevanteBrudd }) {
            "11-8 kan bare registreres med bruddtyper ${relevanteBrudd.joinToString(", ")}"
        }

        //Første brudd i meldeperioden teller ikke i årskvote
        //Deler opp på meldeperiode først for å finne første i meldeperioden
        //Dette brukes for å regne ut antall brudd per kalenderår
        val bruddTidslinjeMedFørsteFraværIdentifisert: Tidslinje<AktivitetspliktSteg1> =
            tidslinjeMedFørsteFraværIdentifisert(resultat, tidslinje)

        val ferdigVurdert = bruddTidslinjeMedFørsteFraværIdentifisert.splittOppKalenderår()
            .flatMap { kalenderårSegment ->
                vurderKalenderår(kalenderårSegment.verdi)
            }

        return resultat.leggTilVurderinger(ferdigVurdert, Vurdering::leggTilAktivitetspliktVurdering)
    }

    private fun tidslinjeMedFørsteFraværIdentifisert(
        resultat: Tidslinje<Vurdering>,
        tidslinje: Tidslinje<AktivitetspliktRegistrering>
    ): Tidslinje<AktivitetspliktSteg1> {
        return groupByMeldeperiode(resultat, tidslinje)
            .flatMap { meldeperiodenSegment ->
                vurderMeldeperiode(meldeperiodenSegment.verdi)
            }
    }

    private fun vurderMeldeperiode(meldeperioden: Tidslinje<AktivitetspliktRegistrering>) =
        meldeperioden.flatMap { bruddSegment ->
            vurderMeldeperiode(meldeperioden, bruddSegment.periode, bruddSegment.verdi)
        }

    private fun vurderMeldeperiode(
        meldeperioden: Tidslinje<AktivitetspliktRegistrering>,
        periode: Periode,
        dokument: AktivitetspliktRegistrering,
    ): Tidslinje<AktivitetspliktSteg1> {
        val inntilEnDagUnntak = meldeperioden.segmenter().firstOrNull {
            it.verdi.grunn !in gyldigeGrunner
        }?.verdi

        val harInntilEnDagUnntak = inntilEnDagUnntak?.metadata?.id == dokument.metadata.id
        return if (harInntilEnDagUnntak) {
            val førsteFravær = Periode(periode.fom, periode.fom)
            val periodene = listOf(førsteFravær) + periode.minus(førsteFravær)
            Tidslinje(
                periodene.map {
                    Segment(
                        it,
                        AktivitetspliktSteg1(
                            dokument = dokument,
                            inntilEnDagUnntak = it == førsteFravær,
                        )
                    )
                }
            )
        } else {
            Tidslinje(
                periode,
                AktivitetspliktSteg1(
                    dokument = dokument,
                    inntilEnDagUnntak = false,
                )
            )
        }
    }

    private fun vurderKalenderår(kalenderår: Tidslinje<AktivitetspliktSteg1>): Tidslinje<FraværFastsattAktivitetVurdering> {
        var kalenderårskvote = 0

        return kalenderår.flatMap { vurderingSegment ->
            val vurdering = vurderingSegment.verdi
            val dokument = vurdering.dokument

            when (dokument.grunn) {
                SYKDOM_ELLER_SKADE ->
                    Tidslinje(
                        vurderingSegment.periode,
                        FraværFastsattAktivitetVurdering(
                            dokument = dokument,
                            vilkårsvurdering = UNNTAK_SYKDOM_ELLER_SKADE,
                        )
                    )

                STERKE_VELFERDSGRUNNER -> {
                    (0..<vurderingSegment.periode.antallDager()).map { periodeOffset ->
                        kalenderårskvote += 1
                        val dag = vurderingSegment.periode.fom.plusDays(periodeOffset.toLong())
                        val periode = Periode(dag, dag)
                        Segment(
                            periode,
                            FraværFastsattAktivitetVurdering(
                                dokument = dokument,
                                vilkårsvurdering = if (kalenderårskvote > KVOTE_KALENDERÅR) STANS_TI_DAGER_BRUKT_OPP else UNNTAK_STERKE_VELFERDSGRUNNER,
                            )
                        )
                    }
                        .let { Tidslinje(it) }
                        .komprimer()
                }

                RIMELIG_GRUNN,
                INGEN_GYLDIG_GRUNN ->
                    Tidslinje(
                        vurderingSegment.periode,
                        FraværFastsattAktivitetVurdering(
                            dokument = dokument,
                            vilkårsvurdering = if (vurdering.inntilEnDagUnntak) UNNTAK_INNTIL_EN_DAG else STANS_ANDRE_DAG,
                        )
                    )
            }
        }
    }


    class AktivitetspliktSteg1(
        val dokument: AktivitetspliktRegistrering,
        val inntilEnDagUnntak: Boolean,
    )
}
