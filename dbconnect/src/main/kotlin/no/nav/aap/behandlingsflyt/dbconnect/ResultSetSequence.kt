package no.nav.aap.behandlingsflyt.dbconnect

import java.sql.ResultSet

internal fun <T> ResultSet.map(block: (rs: ResultSet) -> T): Sequence<T> {
    return mapToSequence(this, block)
}

private fun <T> mapToSequence(resultSet: ResultSet, block: (rs: ResultSet) -> T): Sequence<T> {
    return sequence {
        while (resultSet.next()) {
            yield(block(resultSet))
        }
    }
}
