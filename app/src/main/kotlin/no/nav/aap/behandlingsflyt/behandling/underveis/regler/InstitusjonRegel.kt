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

        val barnetilleggTidslinje = barnetilleggTidslinje(input.barnetillegg)
        institusjonTidslinje =
            institusjonTidslinje.kombiner(barnetilleggTidslinje,
                JoinStyle.LEFT_JOIN { periode, venstreSegment, høyreSegment ->
                    val verdi = venstreSegment.verdi
                    if (høyreSegment != null) {
                        Segment(periode, InstitusjonVurdering(skalReduseres = false, Årsak.BARNETILLEGG.toString(), Årsak.BARNETILLEGG, Prosent.`100_PROSENT`))
                    } else {
                        Segment(periode, verdi)
                    }
                }
            )

        val friForReduksjonTidslinje = friForReduksjonTidslinje(institusjonTidslinje)
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

        return resultat.kombiner(institusjonTidslinje,
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

    // §11-25 avsnitt 2
    private fun friForReduksjonTidslinje(etAnnetStedTidslinje: Tidslinje<InstitusjonVurdering>): Tidslinje<InstitusjonVurdering> {
        val segmenter: List<Segment<InstitusjonVurdering>> = listOf()
        val gyldigForReduksjon = etAnnetStedTidslinje.segmenter().fold(segmenter) { tidligereOpphold, detteOppholdet ->
            val dagFørSegment = Periode(detteOppholdet.fom().minusDays(1), detteOppholdet.tom())
            val erSammeOpphold = etAnnetStedTidslinje.segmenter().filter { it != detteOppholdet }.any{
                it.periode.overlapper(dagFørSegment)
            }

            val periodeTilbake = Periode(detteOppholdet.periode.fom.minusMonths(3), detteOppholdet.periode.fom)
            val overlappTreMnd = tidligereOpphold.filter {
                it.verdi.årsak != Årsak.FORSØRGER &&
                it.verdi.årsak != Årsak.BARNETILLEGG &&
                it.verdi.årsak != Årsak.FASTE_KOSTNADER
                }.any{
                it.periode.overlapper(periodeTilbake)
            }

            if (!overlappTreMnd) {
                val kreativPeriodeTreMnd = Periode(detteOppholdet.fom(), detteOppholdet.fom().plusMonths(3).with(TemporalAdjusters.lastDayOfMonth()))
                tidligereOpphold.plus(Segment(
                    kreativPeriodeTreMnd, InstitusjonVurdering(false, detteOppholdet.verdi.begrunnelse, Årsak.UTEN_REDUKSJON_TRE_MND, Prosent.`100_PROSENT`))
                )
            } else if (!erSammeOpphold) {
                val resterendeMnd = Periode(detteOppholdet.fom(), detteOppholdet.fom().with(TemporalAdjusters.lastDayOfMonth()))
                tidligereOpphold.plus(Segment(
                    resterendeMnd, InstitusjonVurdering(false, detteOppholdet.verdi.begrunnelse, Årsak.UTEN_REDUKSJON_RESTERENDE_MND, Prosent.`100_PROSENT`))
                )
            } else {
                tidligereOpphold
            }
        }
        return Tidslinje(gyldigForReduksjon)
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