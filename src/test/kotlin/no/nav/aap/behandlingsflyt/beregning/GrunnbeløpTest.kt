package no.nav.aap.behandlingsflyt.beregning

import no.nav.aap.behandlingsflyt.Periode
import no.nav.aap.behandlingsflyt.april
import no.nav.aap.behandlingsflyt.desember
import no.nav.aap.behandlingsflyt.faktagrunnlag.inntekt.Beløp
import no.nav.aap.behandlingsflyt.januar
import no.nav.aap.behandlingsflyt.mai
import no.nav.aap.behandlingsflyt.tidslinje.JoinStyle
import no.nav.aap.behandlingsflyt.tidslinje.Segment
import no.nav.aap.behandlingsflyt.tidslinje.StandardSammenslåere
import no.nav.aap.behandlingsflyt.tidslinje.Tidslinje
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
            sammenslåer = StandardSammenslåere.kunHøyre(),
            joinStyle = JoinStyle.LEFT_JOIN
        )

        val periodeForGjennomsnitt: Tidslinje<Any?> = Tidslinje(Periode(31 desember 2009, 1 januar 2010), null)
        val utregnetTidslinjeGjennomsnitt = periodeForGjennomsnitt.kombiner(
            other = tidslinjeGjennomsnitt,
            sammenslåer = StandardSammenslåere.kunHøyre(),
            joinStyle = JoinStyle.LEFT_JOIN
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
