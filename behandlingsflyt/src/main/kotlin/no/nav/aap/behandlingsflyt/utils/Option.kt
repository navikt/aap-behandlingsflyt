package no.nav.aap.behandlingsflyt.utils

import no.nav.aap.komponenter.tidslinje.Tidslinje

sealed interface Option<T>
data class Some<T>(val verdi: T): Option<T>
class None<T>(): Option<T>


private data class Box<T>(val verdi: T)

fun <T> Tidslinje<Option<T>>.mapSome(): Tidslinje<T> {
    return mapNotNull {
        when (it) {
            is None<T> -> null
            is Some<T> -> Box(it.verdi)
        }
    }
        .map { it.verdi }
}