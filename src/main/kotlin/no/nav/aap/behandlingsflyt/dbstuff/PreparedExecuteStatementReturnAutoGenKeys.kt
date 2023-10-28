package no.nav.aap.behandlingsflyt.dbstuff

import java.sql.PreparedStatement

class PreparedExecuteStatementReturnAutoGenKeys(private val preparedStatement: PreparedStatement) {
    fun setParams(block: Params.() -> Unit) {
        Params(preparedStatement).block()
    }

    fun execute(): List<Long> {
        preparedStatement.execute()
        return preparedStatement
            .generatedKeys
            .map { it.getLong(1) }
            .toList()
    }
}
