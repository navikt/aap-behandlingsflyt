package no.nav.aap.behandlingsflyt.underveis.tidslinje

import no.nav.aap.behandlingsflyt.Periode
import java.util.*

class PeriodeIterator<T, E>(
    leftSegments: NavigableSet<Segment<T>>,
    rightSegments: NavigableSet<Segment<E>>
) {

    private val unikePerioder: NavigableSet<Periode> = TreeSet()
    private var dateIterator: Iterator<Periode>

    init {
        unikePerioder.addAll(leftSegments.map { it.periode })
        rightSegments.map { it.periode }.forEach { periode ->
            val overlappendePerioder = unikePerioder.filter { it.overlapper(periode) }
            if (overlappendePerioder.isNotEmpty()) {
                unikePerioder.removeIf { it.overlapper(periode) }
                for (p in overlappendePerioder) {
                    unikePerioder.addAll(periode.minus(p))
                    val overlapp = periode.overlapp(p)
                    if (overlapp != null) {
                        unikePerioder.add(overlapp)
                    }
                    unikePerioder.addAll(p.minus(periode))
                }
            } else {
                unikePerioder.add(periode)
            }
        }

        dateIterator = unikePerioder.iterator()
    }

    fun hasNext(): Boolean {
        return dateIterator.hasNext()
    }

    fun next(): Periode {
        return dateIterator.next()
    }
}