package no.nav.aap.behandlingsflyt.dbstuff

import java.sql.PreparedStatement

class PreparedListQueryStatement<T : Any>(private val preparedStatement: PreparedStatement) {
    private lateinit var rowMapper: (Row) -> T

    fun setParams(block: Params.() -> Unit) {
        Params(preparedStatement).block()
    }

    fun setRowMapper(block: (Row) -> T) {
        rowMapper = block
    }

    fun executeQuery(): List<T> {
        val resultSet = preparedStatement.executeQuery()
        return resultSet
            .map { currentResultSet ->
                rowMapper(Row(currentResultSet))
            }
            .toList()
    }
}
