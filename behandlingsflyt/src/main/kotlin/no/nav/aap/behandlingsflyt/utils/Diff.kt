package no.nav.aap.behandlingsflyt.utils

import no.nav.aap.komponenter.tidslinje.Tidslinje

sealed interface Diff<T> {
    fun <S> map(f: (T) -> S): Diff<S>
    val fraVerdi: Option<T>
    val tilVerdi: Option<T>
}

data class Uendret<T>(val uendret: T) : Diff<T> {
    override fun <S> map(f: (T) -> S) = Uendret(f(uendret))
    override val fraVerdi: Option<T>
        get() = Some(uendret)
    override val tilVerdi: Option<T>
        get() = Some(uendret)
}

data class LagtTil<T>(val lagtTil: T) : Diff<T> {
    override fun <S> map(f: (T) -> S) = LagtTil(f(lagtTil))
    override val fraVerdi: Option<T>
        get() = None()
    override val tilVerdi: Option<T>
        get() = Some(lagtTil)
}

data class Fjernet<T>(val fjernet: T) : Diff<T> {
    override fun <S> map(f: (T) -> S) = Fjernet(f(fjernet))
    override val fraVerdi: Option<T>
        get() = Some(fjernet)
    override val tilVerdi: Option<T>
        get() = None()
}

data class Endret<T>(val fra: T, val til: T) : Diff<T> {
    override fun <S> map(f: (T) -> S) = Endret(fra = f(fra), til = f(til))
    override val fraVerdi: Option<T>
        get() = Some(fra)
    override val tilVerdi: Option<T>
        get() = Some(til)
}

fun <T : Any> diffOf(forrige: T?, nå: T?): Diff<T> {
    return when {
        forrige == null && nå != null -> LagtTil<T>(nå)
        forrige != null && nå == null -> Fjernet<T>(forrige)
        forrige != null && nå != null ->
            if (forrige == nå)
                Uendret<T>(nå)
            else
                Endret<T>(fra = forrige, til = nå)

        else -> error("udefinert verdi både før og etter")
    }
}

fun <T : Any> diffTidslinjer(forrige: Tidslinje<T>, nå: Tidslinje<T>): Tidslinje<Diff<T>> {
    return forrige.outerJoin(nå) { forrigeVerdi, nåverdi ->
        diffOf<T>(forrigeVerdi, nåverdi)
    }
}

fun <T> Tidslinje<Diff<T>>.fraTidslinje(): Tidslinje<T> = map { it.fraVerdi }.mapSome()
fun <T> Tidslinje<Diff<T>>.tilTidslinje(): Tidslinje<T> = map { it.tilVerdi }.mapSome()

fun <K, V> diffMap(forrige: Map<K, V>, nå: Map<K, V>): Map<K, Diff<V>> {
    val resultat = mutableMapOf<K, Diff<V>>()
    for (k in forrige.keys + nå.keys) {
        if (k in forrige && k in nå) {
            val forrigeVerdi = forrige.getValue(k)
            val nåVerdi = nå.getValue(k)
            resultat[k] = if (forrigeVerdi == nåVerdi)
                Uendret(nåVerdi)
            else
                Endret(fra = forrigeVerdi, til = nåVerdi)
        } else {
            resultat[k] = if (k in forrige)
                Fjernet(forrige.getValue(k))
            else
                LagtTil(nå.getValue(k))
        }
    }
    return resultat
}

fun <K, V> Map<K, Diff<V>>.fraMap(): Map<K, V> {
    return entries
        .mapNotNull { (k, diff) ->
            when (val v = diff.fraVerdi) {
                is None<V> -> null
                is Some<V> -> k to v.verdi
            }
        }
        .associate { it }
}

fun <K, V> Map<K, Diff<V>>.tilMap(): Map<K, V> {
    return entries
        .mapNotNull { (k, diff) ->
            when (val v = diff.tilVerdi) {
                is None<V> -> null
                is Some<V> -> k to v.verdi
            }
        }
        .associate { it }
}

