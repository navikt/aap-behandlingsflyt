package no.nav.aap.behandlingsflyt.dbconnect

import org.intellij.lang.annotations.Language
import java.sql.Connection
import java.sql.Savepoint
import java.sql.Statement

class DBConnection(private val connection: Connection) {
    private var savepoint: Savepoint? = null

    fun execute(
        @Language("PostgreSQL")
        query: String,
        block: Execute.() -> Unit = {}
    ) {
        return this.connection.prepareStatement(query).use { preparedStatement ->
            val myPreparedStatement = Execute(preparedStatement)
            myPreparedStatement.block()
            myPreparedStatement.execute()
        }
    }

    fun executeReturnKey(
        @Language("PostgreSQL")
        query: String,
        block: Execute.() -> Unit = {}
    ): Long {
        return this.connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS).use { preparedStatement ->
            val myPreparedStatement = Execute(preparedStatement)
            myPreparedStatement.block()
            return@use myPreparedStatement.executeReturnKey()
        }
    }

    fun executeReturnKeys(
        @Language("PostgreSQL")
        query: String,
        block: Execute.() -> Unit = {}
    ): List<Long> {
        return this.connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS).use { preparedStatement ->
            val myPreparedStatement = Execute(preparedStatement)
            myPreparedStatement.block()
            return@use myPreparedStatement.executeReturnKeys()
        }
    }

    fun <T> queryFirstOrNull(
        @Language("PostgreSQL")
        query: String,
        block: Query<T?>.() -> Unit
    ): T? {
        return this.connection.prepareStatement(query).use { preparedStatement ->
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
        return this.connection.prepareStatement(query).use { preparedStatement ->
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
        return this.connection.prepareStatement(query).use { preparedStatement ->
            val queryStatement = Query<T>(preparedStatement)
            queryStatement.block()
            val result = queryStatement.executeQuery()
            return@use result.toList()
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
