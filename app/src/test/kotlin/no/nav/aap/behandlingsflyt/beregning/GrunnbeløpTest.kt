package no.nav.aap.behandlingsflyt.beregning

import no.nav.aap.behandlingsflyt.dbtestdata.april
import no.nav.aap.behandlingsflyt.dbtestdata.desember
import no.nav.aap.behandlingsflyt.dbtestdata.januar
import no.nav.aap.behandlingsflyt.dbtestdata.mai
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.Grunnbeløp
import no.nav.aap.tidslinje.JoinStyle
import no.nav.aap.tidslinje.Segment
import no.nav.aap.tidslinje.StandardSammenslåere
import no.nav.aap.tidslinje.Tidslinje
import no.nav.aap.verdityper.Beløp
import no.nav.aap.verdityper.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class GrunnbeløpTest {

    @Test
    fun `Genererer tidlinjer for grunnbeløp og gjennomsnittlig grunnbeløp`() {
        val tidslinje = Grunnbeløp.tilTidslinje()
        val tidslinjeGjennomsnitt = Grunnbeløp.tilTidslinjeGjennomsnitt()

        val periodeForTidslinje: Tidslinje<Any?> = Tidslinje(Periode(30 april 2010, 1 mai 2010), null)
        val utregnetTidslinje = periodeForTidslinje.kombiner(
            other = tidslinje,
            joinStyle = JoinStyle.LEFT_JOIN,
            sammenslåer = StandardSammenslåere.kunHøyre()
        )

        val periodeForGjennomsnitt: Tidslinje<Any?> = Tidslinje(Periode(31 desember 2009, 1 januar 2010), null)
        val utregnetTidslinjeGjennomsnitt = periodeForGjennomsnitt.kombiner(
            other = tidslinjeGjennomsnitt,
            joinStyle = JoinStyle.LEFT_JOIN,
            sammenslåer = StandardSammenslåere.kunHøyre()
        )

        assertThat(utregnetTidslinje)
            .containsExactly(
                Segment(Periode(30 april 2010, 30 april 2010), Beløp(72881)),
                Segment(Periode(1 mai 2010, 1 mai 2010), Beløp(75641))
            )
        assertThat(utregnetTidslinjeGjennomsnitt)
            .containsExactly(
                Segment(Periode(31 desember 2009, 31 desember 2009), Beløp(72006)),
                Segment(Periode(1 januar 2010, 1 januar 2010), Beløp(74721))
            )
    }
}
