package no.nav.aap.behandlingsflyt.dbconnect

import java.sql.PreparedStatement

class Execute(private val preparedStatement: PreparedStatement) {
    private var resultValidator: (Int) -> Unit = {}

    fun setParams(block: Params.() -> Unit) {
        Params(preparedStatement).block()
    }

    fun setResultValidator(block: (Int) -> Unit) {
        resultValidator = block
    }

    internal fun execute() {
        val rowsUpdated = preparedStatement.executeUpdate()
        resultValidator(rowsUpdated)
    }

    internal fun executeReturnKey(): Long {
        return executeReturnKeysPrivate().single()
    }

    internal fun executeReturnKeys(): List<Long> {
        return executeReturnKeysPrivate().toList()
    }

    private fun executeReturnKeysPrivate(): Sequence<Long> {
        val rowsUpdated = preparedStatement.executeUpdate()
        resultValidator(rowsUpdated)
        return preparedStatement
            .generatedKeys
            .map { it.getLong(1) }
    }
}
