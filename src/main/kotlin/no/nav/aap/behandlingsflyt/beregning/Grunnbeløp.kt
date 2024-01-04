package no.nav.aap.behandlingsflyt.beregning

import no.nav.aap.behandlingsflyt.Periode
import no.nav.aap.behandlingsflyt.beregning.Grunnbeløp.Element.Companion.finnGUnit
import no.nav.aap.behandlingsflyt.faktagrunnlag.inntekt.Beløp
import no.nav.aap.behandlingsflyt.faktagrunnlag.inntekt.GUnit
import no.nav.aap.behandlingsflyt.tidslinje.Segment
import no.nav.aap.behandlingsflyt.tidslinje.Tidslinje
import java.math.BigDecimal
import java.time.LocalDate
import java.time.Year

internal object Grunnbeløp {
    private val grunnbeløp = listOf(
        Element(2023, 5, 1, 118620, 116239),
        Element(2022, 5, 1, 111477, 109784),
        Element(2021, 5, 1, 106399, 104716),
        Element(2020, 5, 1, 101351, 100853),
        Element(2019, 5, 1, 99858, 98866),
        Element(2018, 5, 1, 96883, 95800),
        Element(2017, 5, 1, 93634, 93281),
        Element(2016, 5, 1, 92576, 91740),
        Element(2015, 5, 1, 90068, 89502),
        Element(2014, 5, 1, 88370, 87328),
        Element(2013, 5, 1, 85245, 84204),
        Element(2012, 5, 1, 82122, 81153),
        Element(2011, 5, 1, 79216, 78024),
        Element(2010, 5, 1, 75641, 74721),
        Element(2009, 5, 1, 72881, 72006),
        Element(2008, 5, 1, 70256, 69108),
        Element(2007, 5, 1, 66812, 65505),
        Element(2006, 5, 1, 62892, 62161),
        Element(2005, 5, 1, 60699, 60059),
        Element(2004, 5, 1, 58778, 58139),
        Element(2003, 5, 1, 56861, 55964),
        Element(2002, 5, 1, 54170, 53233),
        Element(2001, 5, 1, 51360, 50603),
        Element(2000, 5, 1, 49090, 48377),
        Element(1999, 5, 1, 46950, 46423),
        Element(1998, 5, 1, 45370, 44413),
        Element(1997, 5, 1, 42500, 42000),
        Element(1996, 5, 1, 41000, 40410),
        Element(1995, 5, 1, 39230, 38847),
        Element(1994, 5, 1, 38080, 37820),
        Element(1993, 5, 1, 37300, 37033),
        Element(1992, 5, 1, 36500, 36167),
        Element(1991, 5, 1, 35500, 35033),
        Element(1990, 12, 1, 34100, 33575),
        Element(1990, 5, 1, 34000, null),
        Element(1989, 4, 1, 32700, 32275),
        Element(1988, 4, 1, 31000, 30850),
        Element(1988, 1, 1, 30400, null),
        Element(1987, 5, 1, 29900, 29267)
    )

    private class Element(
        år: Int,
        måned: Int,
        dag: Int,
        beløp: Int,
        gjennomsnittBeløp: Int?
    ) {
        private val dato: LocalDate = LocalDate.of(år, måned, dag)
        private val beløp: Beløp = Beløp(BigDecimal(beløp))
        private val gjennomsnittBeløp: Beløp? = gjennomsnittBeløp?.let(::Beløp)

        companion object {
            private fun Iterable<Element>.finnGrunnbeløpForÅr(år: Year): Element {
                return this
                    .filter { it.gjennomsnittBeløp != null }
                    .sortedByDescending { it.dato }
                    .first { år >= Year.from(it.dato) }
            }

            fun Iterable<Element>.finnGUnit(år: Year, beløp: Beløp): GUnit {
                return GUnit(beløp.divitert(nevner = finnGrunnbeløpForÅr(år).gjennomsnittBeløp!!, scale = GUnit.SCALE))
            }

            fun tilTidslinje(): Tidslinje<Beløp> {
                return grunnbeløp
                    .reversed()
                    .zipWithNext { gjeldende, neste ->
                        val periode = Periode(gjeldende.dato, neste.dato.minusDays(1))
                        Segment(periode, gjeldende.beløp)
                    }
                    .plus(Segment(Periode(grunnbeløp.first().dato, LocalDate.MAX), grunnbeløp.first().beløp))
                    .let(::Tidslinje)
            }

            fun tilTidslinjeGjennomsnitt(): Tidslinje<Beløp> {
                return grunnbeløp
                    .filter { it.gjennomsnittBeløp != null }
                    .reversed()
                    .zipWithNext { gjeldende, neste ->
                        val periode = Periode(gjeldende.dato.withDayOfYear(1), neste.dato.withDayOfYear(1).minusDays(1))
                        Segment(periode, gjeldende.gjennomsnittBeløp)
                    }
                    .plus(
                        Segment(
                            Periode(grunnbeløp.first().dato.withDayOfYear(1), LocalDate.MAX),
                            grunnbeløp.first().gjennomsnittBeløp
                        )
                    )
                    .let(::Tidslinje)
            }
        }
    }

    internal fun finnGUnit(år: Year, beløp: Beløp): GUnit {
        return grunnbeløp.finnGUnit(år, beløp)
    }

    internal fun tilTidslinje(): Tidslinje<Beløp> {
        return Element.tilTidslinje()
    }

    internal fun tilTidslinjeGjennomsnitt(): Tidslinje<Beløp> {
        return Element.tilTidslinjeGjennomsnitt()
    }
}
