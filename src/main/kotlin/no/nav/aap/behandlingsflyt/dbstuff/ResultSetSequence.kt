package no.nav.aap.behandlingsflyt.dbstuff

import java.sql.ResultSet

fun <T> ResultSet.map(block: (rs: ResultSet) -> T): Sequence<T> {
    return ResultSetSequence(this).map(block)
}

private class ResultSetSequence(private val resultSet: ResultSet) : Sequence<ResultSet> {
    override fun iterator(): Iterator<ResultSet> {
        return ResultSetIterator()
    }

    private inner class ResultSetIterator : Iterator<ResultSet> {
        private var callHasNext = true
        private var hasNext = false

        override fun hasNext(): Boolean {
            if (callHasNext) {
                callHasNext = false
                hasNext = resultSet.next()
                return hasNext
            }
            return hasNext
        }

        override fun next(): ResultSet {
            callHasNext = true
            return resultSet
        }
    }
}
