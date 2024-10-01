package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.behandlingsflyt.behandling.barnetillegg.RettTilBarnetillegg
import no.nav.aap.behandlingsflyt.behandling.etannetsted.EtAnnetSted
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.barnetillegg.BarnetilleggGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.Gradering
import no.nav.aap.tidslinje.JoinStyle
import no.nav.aap.tidslinje.Segment
import no.nav.aap.tidslinje.Tidslinje
import no.nav.aap.verdityper.Prosent
import java.time.temporal.TemporalAdjusters
import kotlin.math.roundToInt

class InstitusjonRegel : UnderveisRegel {
    override fun vurder(input: UnderveisInput, resultat: Tidslinje<Vurdering>): Tidslinje<Vurdering> {
        // TODO
        val intitusjonTidslinje = konstruerTidslinje(input)
        val barnetilleggGrunnlagTidslinje = input.barnetillegg?.let { barnetilleggTidslinje(it) }
        val friForReduksjonTidslinje = friForReduksjonTidslinje(intitusjonTidslinje)

        val nyttResultat = resultat.kombiner(intitusjonTidslinje,
            JoinStyle.LEFT_JOIN { periode, venstreSegment, høyreSegment ->
                val venstreVerdi = venstreSegment.verdi

                if (høyreSegment?.verdi != null && høyreSegment.verdi.institusjon.erPåInstitusjon) {
                    val harBarnetillegg = true
                    if (!høyreSegment.verdi.institusjon.forsørgerEktefelle && !harBarnetillegg) {
                        //TODO: IF har du innleggelsesmåneden og de tre påfølgende månedene? -FRI FOR REDUKSJON

                        //TODO: IF har du barnetillegg eller forsørger noen -> FRI FOR REDUKSJON
                        //TODO: 3 Første mnd -> FRI FOR REDUKSJON
                            // MEN, Dersom medlemmet innen tre måneder
                            // etter utskrivelsen på nytt kommer i institusjon, gis det reduserte
                            // arbeidsavklaringspenger fra og
                            // med måneden etter at det nye oppholdet tar til.



                        /*

                        //REDUSERT
                        if (originalGradering != null) {
                            venstreVerdi.leggTilGradering(
                                Gradering(
                                    originalGradering.totaltAntallTimer,
                                    originalGradering.andelArbeid,
                                    Prosent(originalGradering.gradering.prosentverdi().times(0.5).roundToInt())
                                )
                            )
                        }


*/


                    }
                }
                Segment(periode, venstreVerdi)
            })

        return nyttResultat
    }

    private fun barnetilleggTidslinje(barnetilleggGrunnlag: BarnetilleggGrunnlag): Tidslinje<RettTilBarnetillegg> {
        return Tidslinje(
            barnetilleggGrunnlag.perioder.map {
                Segment(
                    it.periode,
                    RettTilBarnetillegg(
                        it.personIdenter
                    )
                )
            }
        )
    }

    private fun friForReduksjonTidslinje(etAnnetStedTidslinje: Tidslinje<EtAnnetSted>): Tidslinje<EtAnnetSted> {
        val resterendeMndPlussTre = etAnnetStedTidslinje.filter { segment ->
            val reduksjonLimit = etAnnetStedTidslinje.minDato().plusMonths(3).with(TemporalAdjusters.lastDayOfMonth())

            // Hva hvis fom periode og tom periode er på hver side?
            segment.periode.fom > reduksjonLimit
        }

        return Tidslinje(
            resterendeMndPlussTre.map {
                Segment(it.periode, it.verdi)
            }
        )
    }

    private fun konstruerTidslinje(input: UnderveisInput): Tidslinje<EtAnnetSted> {
        return Tidslinje(
            input.etAnnetSted.filter {
                it.institusjon.erPåInstitusjon
            }.map {
                Segment(it.periode, EtAnnetSted(it.periode, it.soning, it.institusjon))
            }
        )
    }
}