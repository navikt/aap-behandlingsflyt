package no.nav.aap.behandlingsflyt.behandling.beregning

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.Grunnbeløp
import no.nav.aap.behandlingsflyt.test.april
import no.nav.aap.behandlingsflyt.test.desember
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.behandlingsflyt.test.mai
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.StandardSammenslåere
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Beløp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Year

class GrunnbeløpTest {

    @Test
    fun `Genererer tidslinje for gjennomsnittlig grunnbeløp`() {
        val tidslinjeGjennomsnitt = Grunnbeløp.tilTidslinjeGjennomsnitt()

        val periodeForGjennomsnitt: Tidslinje<Any?> = Tidslinje(Periode(31 desember 2009, 1 januar 2010), null)
        val utregnetTidslinjeGjennomsnitt = periodeForGjennomsnitt.kombiner(
            other = tidslinjeGjennomsnitt,
            joinStyle = StandardSammenslåere.kunHøyre()
        )

        assertThat(utregnetTidslinjeGjennomsnitt.segmenter())
            .containsExactly(
                Segment(Periode(31 desember 2009, 31 desember 2009), Beløp(72006)),
                Segment(Periode(1 januar 2010, 1 januar 2010), Beløp(74721))
            )
    }

    @Test
    fun `Genererer tidslinje for grunnbeløp`() {
        val tidslinje = Grunnbeløp.tilTidslinje()

        val periodeForTidslinje: Tidslinje<Any?> = Tidslinje(Periode(30 april 2010, 1 mai 2010), null)
        val utregnetTidslinje = periodeForTidslinje.kombiner(
            other = tidslinje,
            joinStyle = StandardSammenslåere.kunHøyre()
        )

        assertThat(utregnetTidslinje.segmenter())
            .containsExactly(
                Segment(Periode(30 april 2010, 30 april 2010), Beløp(72881)),
                Segment(Periode(1 mai 2010, 1 mai 2010), Beløp(75641))
            )
    }

    @Test
    fun `fornuftig feilmelding om man prøver å slå opp grunnbeløp som ikke finnes`() {
        val exception = assertThrows<RuntimeException> { Grunnbeløp.finnGUnit(Year.of(1814), Beløp(1000)) }

        assertThat(exception.message).contains("Finner ikke gjennomsnittsbeløp for dato: 1814-01-01.")
    }

    @Test
    fun `g-unit ganget med finnGUnit gir tilbake beløp`() {
        val finnGUnit = Grunnbeløp.finnGUnit(Year.of(1979), Beløp(1000))

        assertThat(finnGUnit.gUnit.multiplisert(Beløp(15200))).isEqualTo(Beløp(1000))
    }
}
