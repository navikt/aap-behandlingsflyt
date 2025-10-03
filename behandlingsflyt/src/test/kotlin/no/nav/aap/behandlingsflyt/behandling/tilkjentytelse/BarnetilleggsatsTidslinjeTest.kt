package no.nav.aap.behandlingsflyt.behandling.tilkjentytelse

import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.verdityper.Beløp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class BarnetilleggsatsTidslinjeTest {
    @Test
    fun `tidslinja er kontinuerlig`() {
        val perioder = BARNETILLEGGSATS_TIDSLINJE.segmenter().map(Segment<Beløp>::periode)

        assertThat(perioder.first().fom).isEqualTo(LocalDate.MIN)
        assertThat(perioder.last().tom).isEqualTo(LocalDate.MAX)

        assertThat(perioder.zipWithNext()).allMatch { (current, next) ->
            current.tom.plusDays(1) == next.fom
        }
    }
}