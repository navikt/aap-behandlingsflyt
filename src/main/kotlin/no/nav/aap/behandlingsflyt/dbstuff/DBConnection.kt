package no.nav.aap.behandlingsflyt.dbstuff

import java.sql.Connection
import java.sql.Savepoint
import java.sql.Statement

class DBConnection(private val connection: Connection) {
    private var savepoint: Savepoint? = null

    fun prepareExecuteStatement(
        query: String,
        block: PreparedExecuteStatement.() -> Unit
    ) {
        return this.connection.prepareStatement(query).use { preparedStatement ->
            val myPreparedStatement = PreparedExecuteStatement(preparedStatement)
            myPreparedStatement.block()
            myPreparedStatement.execute()
        }
    }

    fun prepareExecuteStatementReturnAutoGenKeys(
        query: String,
        block: PreparedExecuteStatementReturnAutoGenKeys.() -> Unit
    ): List<Long> {
        return this.connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS).use { preparedStatement ->
            val myPreparedStatement = PreparedExecuteStatementReturnAutoGenKeys(preparedStatement)
            myPreparedStatement.block()
            myPreparedStatement.execute()
        }
    }

    fun <T : Any> prepareFirstOrNullQueryStatement(
        query: String,
        block: PreparedFirstOrNullQueryStatement<T>.() -> Unit
    ): T? {
        return this.connection.prepareStatement(query).use { preparedStatement ->
            val preparedQueryStatement = PreparedFirstOrNullQueryStatement<T>(preparedStatement)
            preparedQueryStatement.block()
            preparedQueryStatement.executeQuery()
        }
    }

    fun <T : Any> prepareFirstQueryStatement(
        query: String,
        block: PreparedFirstQueryStatement<T>.() -> Unit
    ): T {
        return this.connection.prepareStatement(query).use { preparedStatement ->
            val preparedQueryStatement = PreparedFirstQueryStatement<T>(preparedStatement)
            preparedQueryStatement.block()
            preparedQueryStatement.executeQuery()
        }
    }

    fun <T : Any> prepareListQueryStatement(
        query: String,
        block: PreparedListQueryStatement<T>.() -> Unit
    ): List<T> {
        return this.connection.prepareStatement(query).use { preparedStatement ->
            val preparedQueryStatement = PreparedListQueryStatement<T>(preparedStatement)
            preparedQueryStatement.block()
            preparedQueryStatement.executeQuery()
        }
    }

    fun markerSavepoint() {
        savepoint = this.connection.setSavepoint()
    }

    fun rollback() {
        if (savepoint != null) {
            this.connection.rollback(savepoint)
        } else {
            this.connection.rollback()
        }
    }
}
