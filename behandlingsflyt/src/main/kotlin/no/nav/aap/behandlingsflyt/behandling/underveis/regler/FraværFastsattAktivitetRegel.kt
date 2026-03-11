package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.behandlingsflyt.behandling.underveis.regler.FraværFastsattAktivitetVurdering.Vilkårsvurdering.STANS_ANDRE_DAG
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.FraværFastsattAktivitetVurdering.Vilkårsvurdering.STANS_TI_DAGER_BRUKT_OPP
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.FraværFastsattAktivitetVurdering.Vilkårsvurdering.UNNTAK_INNTIL_EN_DAG
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.FraværFastsattAktivitetVurdering.Vilkårsvurdering.UNNTAK_STERKE_VELFERDSGRUNNER
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.FraværFastsattAktivitetVurdering.Vilkårsvurdering.UNNTAK_SYKDOM_ELLER_SKADE
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.FraværForDag
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.FraværÅrsak
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.somTidslinje
import no.nav.aap.komponenter.type.Periode

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
        private val gyldigeGrunner = listOf(
//            STERKE_VELFERDSGRUNNER, // TODO denne eller alle under?
            FraværÅrsak.OMSORG_FØRSTE_SKOLEDAG_TILVENNING_ELLER_ANNEN_OPPFØLGING_BARN,
            FraværÅrsak.OMSORG_PLEIE_I_HJEMMET_AV_NÆR_PÅRØRENDE,
            FraværÅrsak.OMSORG_DØDSFALL_I_FAMILIE_ELLER_VENNEKRETS,
            FraværÅrsak.OMSORG_ANNEN_STERK_GRUNN,

            FraværÅrsak.SYKDOM_ELLER_SKADE,
        )
    }

    override fun vurder(input: UnderveisInput, resultat: Tidslinje<Vurdering>): Tidslinje<Vurdering> {
        require(input.periodeForVurdering.inneholder(resultat.helePerioden())) {
            "kan ikke vurdere utenfor periode for vurdering fordi meldeperioden ikke er definert"
        }

        val sisteFjortenDager = TODO();
        val inneværendeKalenderÅrOpptilDagensDato = TODO();

        val tidslinje: Tidslinje<FraværForDag> =
            input.meldekort
                .sortedBy { it.mottattTidspunkt } // somTidslinje vil overskrive tidligere verdier
                .flatMap { it.fravær }
                .somTidslinje { Periode(it.dato, it.dato) }

        // Første brudd i meldeperioden teller ikke i årskvote
        // Deler opp på meldeperiode først for å finne første i meldeperioden
        // Dette brukes for å regne ut antall brudd per kalenderår
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
        tidslinje: Tidslinje<FraværForDag>,
    ): Tidslinje<AktivitetspliktSteg1> {
        return groupByMeldeperiode(resultat, tidslinje)
            .flatMap { meldeperiodenSegment ->
                vurderMeldeperiode(meldeperiodenSegment.verdi)
            }
    }

    private fun groupByMeldeperiode(
        resultat: Tidslinje<Vurdering>,
        tidslinje: Tidslinje<FraværForDag>,
    ): Tidslinje<Tidslinje<FraværForDag>> {
        // TODO trolig en annen måte for dette nå
        return tidslinje.splittOppIPerioder(resultat.map { vurdering ->
            Periode(
                fom = maxOf(vurdering.meldeperiode().fom, resultat.minDato()),
                tom = minOf(vurdering.meldeperiode().tom, resultat.maxDato()),
            )
        }.komprimer().segmenter().map { it.verdi })
    }

    private fun vurderMeldeperiode(meldeperioden: Tidslinje<FraværForDag>) =
        meldeperioden.flatMap { bruddSegment ->
            vurderMeldeperiode(meldeperioden, bruddSegment.periode, bruddSegment.verdi)
        }

    private fun vurderMeldeperiode(
        meldeperioden: Tidslinje<FraværForDag>,
        periode: Periode,
        dokument: FraværForDag,
    ): Tidslinje<AktivitetspliktSteg1> {
        val inntilEnDagUnntak = meldeperioden.segmenter().firstOrNull {
            it.verdi.fraværÅrsak !in gyldigeGrunner
        }?.verdi

        val harInntilEnDagUnntak = inntilEnDagUnntak == dokument
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

            when (dokument.fraværÅrsak) {
                FraværÅrsak.SYKDOM_ELLER_SKADE ->
                    Tidslinje(
                        vurderingSegment.periode,
                        FraværFastsattAktivitetVurdering(
                            dokument = dokument,
                            vilkårsvurdering = UNNTAK_SYKDOM_ELLER_SKADE,
                        )
                    )

                FraværÅrsak.OMSORG_FØRSTE_SKOLEDAG_TILVENNING_ELLER_ANNEN_OPPFØLGING_BARN,
                FraværÅrsak.OMSORG_PLEIE_I_HJEMMET_AV_NÆR_PÅRØRENDE,
                FraværÅrsak.OMSORG_DØDSFALL_I_FAMILIE_ELLER_VENNEKRETS,
                FraværÅrsak.OMSORG_ANNEN_STERK_GRUNN -> {
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

                FraværÅrsak.ANNET ->
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
        val dokument: FraværForDag,
        val inntilEnDagUnntak: Boolean,
    )
}
