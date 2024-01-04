package no.nav.aap.behandlingsflyt.tidslinje

import no.nav.aap.behandlingsflyt.Periode
import java.util.*

class PeriodeIterator(
    leftPerioder: NavigableSet<Periode>,
    rightPerioder: NavigableSet<Periode>
) : Iterator<Periode> {

    private val unikePerioder: NavigableSet<Periode> = TreeSet()
    private var dateIterator: Iterator<Periode>

    init {
        val temp = TreeSet(leftPerioder + rightPerioder)
        temp.forEach { periode ->
            val overlappendePerioder = unikePerioder.filter { it.overlapper(periode) }
            if (overlappendePerioder.isNotEmpty()) {
                unikePerioder.removeAll(overlappendePerioder.toSet())
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

    override fun hasNext(): Boolean {
        return dateIterator.hasNext()
    }

    override fun next(): Periode {
        return dateIterator.next()
    }
}