package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.behandlingsflyt.behandling.barnetillegg.RettTilBarnetillegg
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
        var institusjonTidslinje = konstruerTidslinje(input)
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
                        høyreSegment.verdi.årsak
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
                        Segment(periode, InstitusjonVurdering(skalReduseres = false, Årsak.BARNETILLEGG.toString(), Årsak.BARNETILLEGG ))
                    } else {
                        Segment(periode, verdi)
                    }
                }
            )

        val endeligResultat = resultat.kombiner(institusjonTidslinje,
            JoinStyle.LEFT_JOIN { periode, venstreSegment, høyreSegment ->
                val venstreVerdi = venstreSegment.verdi
                if (høyreSegment?.verdi != null) {
                    venstreVerdi.leggTilInstitusjonVurdering(høyreSegment.verdi)
                    val originalGradering = requireNotNull(venstreVerdi.gradering())

                    if (høyreSegment.verdi.skalReduseres) {
                        venstreVerdi.leggTilGradering(
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
        val resterendeMndPlussTre = etAnnetStedTidslinje.filter { segment ->
            val reduksjonLimit = etAnnetStedTidslinje.minDato().plusMonths(3).with(TemporalAdjusters.lastDayOfMonth())

            //TODO: Hva hvis fom periode og tom periode er på hver side?
            segment.periode.fom > reduksjonLimit
        }

        return Tidslinje(
            resterendeMndPlussTre.map {
                Segment(it.periode, InstitusjonVurdering(skalReduseres = false, it.verdi.begrunnelse, årsak = Årsak.UTEN_REDUKSJON_TRE_MND))
            }
        )
    }

    private fun konstruerTidslinje(input: UnderveisInput): Tidslinje<InstitusjonVurdering> {
        return Tidslinje(
            input.etAnnetSted.filter {
                it.institusjon.erPåInstitusjon
            }.map {
                if (it.institusjon.forsørgerEktefelle) {
                    Segment(it.periode, InstitusjonVurdering(skalReduseres = false, it.begrunnelse, Årsak.FORSØRGER))
                } else {
                    Segment(it.periode, InstitusjonVurdering(skalReduseres = true, it.begrunnelse, null))
                }
            }
        )
    }
}