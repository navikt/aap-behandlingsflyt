package no.nav.aap.tidslinje

import no.nav.aap.verdityper.Periode
import java.util.*

class PeriodeIterator(
    leftPerioder: NavigableSet<Periode>,
    rightPerioder: NavigableSet<Periode>
) : Iterator<Periode> {

    private var unikePerioder: NavigableSet<Periode> = TreeSet()
    private var dateIterator: Iterator<Periode>

    init {
        val temp = TreeSet(leftPerioder + rightPerioder)
        // Kan traverse lineær, siden TreeSet allerede har sortert.
        // Se https://stackoverflow.com/a/9775727/1013553
        // for algoritmen.
        unikePerioder = temp.fold(unikePerioder) { acc, curr ->
            // At beginning
            if (acc.size < 1) {
                acc.add(curr)
                acc
            } else {
                val last = acc.last
                if (acc.last.overlapper(curr)) {
                    acc.remove(last)
                    acc.addAll(last.minus(curr))
                    acc.add(curr.overlapp(last))
                    acc.addAll(curr.minus(last))
                } else {
                    acc.add(curr)
                }
                acc
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