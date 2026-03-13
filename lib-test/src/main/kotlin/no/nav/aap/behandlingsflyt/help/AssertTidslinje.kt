package no.nav.aap.behandlingsflyt.help

import no.nav.aap.komponenter.tidslinje.JoinStyle
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.somTidslinje
import no.nav.aap.komponenter.type.Periode
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.opentest4j.AssertionFailedError

inline fun <reified T> Tidslinje<T>.assertTidslinje(assertions: Tidslinje<(T) -> Unit>) {
    this.kombiner(assertions, JoinStyle.OUTER_JOIN<_, _, Nothing?> { periode, tsegment, assertion ->
        Assertions.assertNotNull(tsegment, "Verdi av type ${T::class.simpleName} mangler for periode $periode")
        Assertions.assertNotNull(assertion, "Assert mangler for periode $periode (verdi ${tsegment!!.verdi})")

        try {
            assertion!!.verdi.invoke(tsegment!!.verdi)
        } catch (e: java.lang.AssertionError) {
            throw java.lang.AssertionError("for periode $periode: ${e.message}", e)
        } catch (e: AssertionFailedError) {
            println("Assert feilet for periode: $periode")
            throw e
        }
        null
    }
    )
}

inline fun <reified T> Tidslinje<T>.assertTidslinje(vararg assertions: Segment<(T) -> Unit>) {
    this.assertTidslinje(Tidslinje(assertions.toList()))
}

inline fun <reified T> assertTidslinje(tidslinje: Tidslinje<T>, vararg assertions: Pair<Periode, (T) -> Unit>) {
    tidslinje.assertTidslinje(*assertions.map { (periode, assertion) -> Segment(periode, assertion) }.toTypedArray())
}

inline fun <reified T> assertTidslinjeEquals(actualTidslinje: Tidslinje<T>, expectedTidslinje: Tidslinje<T>, message: String? = null) {
    val assertions: Tidslinje<(T) -> Unit> = expectedTidslinje.map { expected ->
        { actual ->
            assertEquals(expected, actual, message)
        }
    }

    actualTidslinje.assertTidslinje(assertions)
}

inline fun <reified T> assertTidslinjeEquals(actualTidslinje: Tidslinje<T>, vararg expected: Pair<Periode, T>) {
    assertTidslinjeEquals(actualTidslinje, expected.toList().somTidslinje({  it.first }, { it.second }))
}
