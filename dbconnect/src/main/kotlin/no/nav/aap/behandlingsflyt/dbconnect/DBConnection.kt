package no.nav.aap.behandlingsflyt.dbconnect

import org.intellij.lang.annotations.Language
import java.sql.Connection
import java.sql.Savepoint
import java.sql.Statement

private const val QUERY_TIMEOUT_IN_SECONDS = 90

class DBConnection internal constructor(private val connection: Connection) {
    private var savepoint: Savepoint? = null

    fun execute(
        @Language("PostgreSQL")
        query: String,
        block: Execute.() -> Unit = {}
    ) {
        val prepareStatement = this.connection.prepareStatement(query)
        prepareStatement.queryTimeout = QUERY_TIMEOUT_IN_SECONDS
        return prepareStatement.use { preparedStatement ->
            val executeStatement = Execute(preparedStatement)
            executeStatement.block()
            executeStatement.execute()
        }
    }

    fun executeReturnKey(
        @Language("PostgreSQL")
        query: String,
        block: Execute.() -> Unit = {}
    ): Long {
        val prepareStatement = this.connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)
        prepareStatement.queryTimeout = QUERY_TIMEOUT_IN_SECONDS
        return prepareStatement.use { preparedStatement ->
            val executeStatement = Execute(preparedStatement)
            executeStatement.block()
            return@use executeStatement.executeReturnKey()
        }
    }

    fun executeReturnKeys(
        @Language("PostgreSQL")
        query: String,
        block: Execute.() -> Unit = {}
    ): List<Long> {
        val prepareStatement = this.connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)
        prepareStatement.queryTimeout = QUERY_TIMEOUT_IN_SECONDS
        return prepareStatement.use { preparedStatement ->
            val executeStatement = Execute(preparedStatement)
            executeStatement.block()
            return@use executeStatement.executeReturnKeys()
        }
    }

    fun <T> queryFirstOrNull(
        @Language("PostgreSQL")
        query: String,
        block: Query<T?>.() -> Unit
    ): T? {
        val prepareStatement = this.connection.prepareStatement(query)
        prepareStatement.queryTimeout = QUERY_TIMEOUT_IN_SECONDS
        return prepareStatement.use { preparedStatement ->
            val queryStatement = Query<T?>(preparedStatement)
            queryStatement.block()
            val result = queryStatement.executeQuery()
            return@use result.firstOrNull()
        }
    }

    fun <T : Any> queryFirst(
        @Language("PostgreSQL")
        query: String,
        block: Query<T>.() -> Unit
    ): T {
        val prepareStatement = this.connection.prepareStatement(query)
        prepareStatement.queryTimeout = QUERY_TIMEOUT_IN_SECONDS
        return prepareStatement.use { preparedStatement ->
            val queryStatement = Query<T>(preparedStatement)
            queryStatement.block()
            val result = queryStatement.executeQuery()
            return@use result.first()
        }
    }

    fun <T : Any> queryList(
        @Language("PostgreSQL")
        query: String,
        block: Query<T>.() -> Unit
    ): List<T> {
        val prepareStatement = this.connection.prepareStatement(query)
        prepareStatement.queryTimeout = QUERY_TIMEOUT_IN_SECONDS
        return prepareStatement.use { preparedStatement ->
            val queryStatement = Query<T>(preparedStatement)
            queryStatement.block()
            val result = queryStatement.executeQuery()
            return@use result.toList()
        }
    }

    fun markerSavepoint() {
        savepoint = this.connection.setSavepoint()
    }

    internal fun rollback() {
        if (savepoint != null) {
            this.connection.rollback(savepoint)
        } else {
            this.connection.rollback()
        }
    }

    internal fun commit() {
        this.connection.commit()
    }

    internal fun autoCommitOn() {
        this.connection.autoCommit = true
    }

    internal fun autoCommitOff() {
        this.connection.autoCommit = false
    }
}
