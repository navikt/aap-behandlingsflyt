package no.nav.aap.behandlingsflyt.behandling.tilkjentytelse

import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.verdityper.GUnit
import no.nav.aap.komponenter.verdityper.Tid
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class MinsteÅrligYtelseTest {
    @Test
    fun `tidslinja er kontinuerlig`() {
        val perioder = MINSTE_ÅRLIG_YTELSE_TIDSLINJE.segmenter().map(Segment<GUnit>::periode)

        assertThat(perioder.first().fom).isEqualTo(LocalDate.MIN)
        assertThat(perioder.last().tom).isEqualTo(Tid.MAKS)

        assertThat(perioder.zipWithNext()).allMatch { (current, next) ->
            current.tom.plusDays(1) == next.fom
        }
    }
}