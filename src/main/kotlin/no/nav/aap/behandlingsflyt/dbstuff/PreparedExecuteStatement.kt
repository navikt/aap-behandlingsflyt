package no.nav.aap.behandlingsflyt.dbstuff

import java.sql.PreparedStatement

class PreparedExecuteStatement(private val preparedStatement: PreparedStatement) {
    private var resultValidator: (Int) -> Unit = {}

    fun setParams(block: Params.() -> Unit) {
        Params(preparedStatement).block()
    }

    fun setResultValidator(block: (Int) -> Unit) {
        resultValidator = block
    }

    fun execute() {
        val rowsUpdated = preparedStatement.executeUpdate()
        resultValidator(rowsUpdated)
    }
}
