package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.komponenter.type.Periode
import no.nav.aap.tidslinje.JoinStyle
import no.nav.aap.tidslinje.Segment
import no.nav.aap.tidslinje.Tidslinje
import org.slf4j.LoggerFactory

class UtledMeldeperiodeRegel: UnderveisRegel {
    companion object {
        const val MELDEPERIODE_LENGDE: Long = 14
        private val log = LoggerFactory.getLogger(Companion::class.java)!!

        fun <T> groupByMeldeperiode(
            vurderinger: Tidslinje<Vurdering>,
            tidslinje: Tidslinje<T>,
        ): Tidslinje<Tidslinje<T>> {
            class VerdiMedMeldeperiode<T>(val meldeperiode: Periode, val tVerdi: T)

            val meldeperioder: Tidslinje<Periode> = vurderinger
                .mapNotNull { segment -> segment.verdi.meldeperiode() }
                .map { meldeperiode -> Segment(meldeperiode, meldeperiode) }
                .let { Tidslinje(it) }

            val verdierMedMeldeperiodeTidslinje: Tidslinje<VerdiMedMeldeperiode<T>> =
                meldeperioder.kombiner(tidslinje, JoinStyle.RIGHT_JOIN { periode, meldeperiode, tSegment ->
                    if (meldeperiode == null) {
                        log.warn("mangler meldeperiode for gruppering, verdi ")
                        null
                    } else {
                        Segment(periode, VerdiMedMeldeperiode(meldeperiode.verdi, tSegment.verdi))
                    }
                })

            return verdierMedMeldeperiodeTidslinje
                .groupBy(
                    { segment -> segment.verdi.meldeperiode },
                    { segment -> Segment(segment.periode, segment.verdi.tVerdi) }
                )
                .map { (meldeperiode, listMedSegmenter) ->
                    Segment(meldeperiode, Tidslinje(listMedSegmenter))
                }
                .let { Tidslinje(it) }
        }
    }

    override fun vurder(input: UnderveisInput, resultat: Tidslinje<Vurdering>): Tidslinje<Vurdering> {
        val rettighetsperiode = input.rettighetsperiode
        val meldeperiodeTidslinje = generateSequence(rettighetsperiode.fom) { it.plusDays(MELDEPERIODE_LENGDE) }
            .map { meldeperiodeStart ->
                val meldeperiode = Periode(meldeperiodeStart, meldeperiodeStart.plusDays(MELDEPERIODE_LENGDE-1))
                Segment(meldeperiode, meldeperiode)
            }
            .takeWhile { it.periode.tom <= rettighetsperiode.tom }.toList() //TODO - hva skjer hvis rettighetsperioden.dager%14 != 0

        return resultat.leggTilVurderinger(Tidslinje(meldeperiodeTidslinje), Vurdering::leggTilMeldeperiode)
    }
}