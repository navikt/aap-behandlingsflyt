package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.behandlingsflyt.behandling.barnetillegg.RettTilBarnetillegg
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.barnetillegg.BarnetilleggGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.Gradering
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.tidslinje.JoinStyle
import no.nav.aap.tidslinje.Segment
import no.nav.aap.tidslinje.Tidslinje
import no.nav.aap.verdityper.Prosent
import java.time.temporal.TemporalAdjusters
import kotlin.math.roundToInt

class InstitusjonRegel : UnderveisRegel {
    override fun vurder(input: UnderveisInput, resultat: Tidslinje<Vurdering>): Tidslinje<Vurdering> {
        var institusjonTidslinje = konstruerTidslinje(input)
        if (institusjonTidslinje.segmenter().size < 1) {
            return resultat
        }

        val friForReduksjonTidslinje = friForReduksjonTidslinje(institusjonTidslinje)
        val barnetilleggTidslinje = barnetilleggTidslinje(input.barnetillegg)

        institusjonTidslinje =
            institusjonTidslinje.kombiner(friForReduksjonTidslinje,
            JoinStyle.LEFT_JOIN { periode, venstreSegment, høyreSegment ->
                val verdi = venstreSegment.verdi
                if (høyreSegment != null) {
                    Segment(periode, InstitusjonVurdering(
                        skalReduseres = høyreSegment.verdi.skalReduseres,
                        høyreSegment.verdi.begrunnelse,
                        høyreSegment.verdi.årsak,
                        Prosent.`100_PROSENT`
                        )
                    )
                } else {
                    Segment(periode, verdi)
                }
            }
        )

        institusjonTidslinje =
            institusjonTidslinje.kombiner(barnetilleggTidslinje,
                JoinStyle.LEFT_JOIN { periode, venstreSegment, høyreSegment ->
                    val verdi = venstreSegment.verdi
                    if (høyreSegment != null) {
                        Segment(periode, InstitusjonVurdering(skalReduseres = false, Årsak.BARNETILLEGG.toString(), Årsak.BARNETILLEGG, Prosent.`100_PROSENT` ))
                    } else {
                        Segment(periode, verdi)
                    }
                }
            )

        val endeligResultat = resultat.kombiner(institusjonTidslinje,
            JoinStyle.LEFT_JOIN { periode, venstreSegment, høyreSegment ->
                var venstreVerdi = venstreSegment.verdi
                if (høyreSegment?.verdi != null) {
                    venstreVerdi = venstreVerdi.leggTilInstitusjonVurdering(høyreSegment.verdi)
                    val originalGradering = requireNotNull(venstreVerdi.gradering())

                    if (høyreSegment.verdi.skalReduseres) {
                        venstreVerdi = venstreVerdi.leggTilGradering(
                            Gradering(
                                originalGradering.totaltAntallTimer,
                                originalGradering.andelArbeid,
                                Prosent(originalGradering.gradering.prosentverdi().times(0.5).roundToInt())
                            )
                        )
                    }
                }

                Segment(periode, venstreVerdi)
                }
            )

        return endeligResultat
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

    //TODO: Denne tar foreløpig ikke høyde for om medlemmet innen tre måneder etter utskrivelsen på nytt kommer i institusjon
    private fun friForReduksjonTidslinje(etAnnetStedTidslinje: Tidslinje<InstitusjonVurdering>): Tidslinje<InstitusjonVurdering> {
        val kretivPeriode = Periode(etAnnetStedTidslinje.minDato(), etAnnetStedTidslinje.minDato().plusMonths(3).with(TemporalAdjusters.lastDayOfMonth()))
        val treMndTidslinje = Tidslinje(kretivPeriode, null)

        return etAnnetStedTidslinje.kombiner(treMndTidslinje,
            JoinStyle.INNER_JOIN { periode, venstreSegment, høyreSegment ->
                Segment(periode, InstitusjonVurdering(skalReduseres = false, venstreSegment.verdi.begrunnelse, årsak = Årsak.UTEN_REDUKSJON_TRE_MND, Prosent.`100_PROSENT`))
            }
        )
    }

    private fun konstruerTidslinje(input: UnderveisInput): Tidslinje<InstitusjonVurdering> {
        return Tidslinje(
            input.etAnnetSted.filter {
                it.institusjon.erPåInstitusjon
            }.map {
                if (it.institusjon.forsørgerEktefelle) {
                    Segment(it.periode, InstitusjonVurdering(skalReduseres = false, it.begrunnelse, Årsak.FORSØRGER, Prosent.`100_PROSENT`))
                }
                else if(it.institusjon.harFasteKostnader) {
                    Segment(it.periode, InstitusjonVurdering(skalReduseres = false, it.begrunnelse, Årsak.FASTE_KOSTNADER, Prosent.`100_PROSENT`))
                }
                else {
                    Segment(it.periode, InstitusjonVurdering(skalReduseres = true, it.begrunnelse, Årsak.KOST_OG_LOSJI, Prosent.`50_PROSENT`))
                }
            }
        )
    }
}