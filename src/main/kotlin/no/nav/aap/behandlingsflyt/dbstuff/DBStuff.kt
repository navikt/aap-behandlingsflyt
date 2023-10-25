package no.nav.aap.behandlingsflyt.dbstuff

import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.LocalDateTime
import java.util.*
import javax.sql.DataSource

private class ResultSetSequence(private val resultSet: ResultSet) : Sequence<ResultSet> {
    override fun iterator(): Iterator<ResultSet> {
        return ResultSetIterator()
    }

    private inner class ResultSetIterator : Iterator<ResultSet> {
        override fun hasNext(): Boolean {
            return resultSet.next()
        }

        override fun next(): ResultSet {
            return resultSet
        }
    }
}

fun <T : Any> ResultSet.map(block: (rs: ResultSet) -> T): Sequence<T> {
    return ResultSetSequence(this).map(block)
}

fun <T> DataSource.connect(block: DbConnection.() -> T): T {
    return this.connection.use { connection ->
        DbConnection(connection).block()
    }
}

fun <T> DataSource.transaction(block: DbConnection.() -> T): T {
    return this.connection.use { connection ->
        try {
            connection.autoCommit = false
            val result = DbConnection(connection).block()
            connection.commit()
            result
        } catch (e: Throwable) {
            connection.rollback()
            throw e
        } finally {
            connection.autoCommit = true
        }
    }
}

class DbConnection(private val connection: Connection) {
    fun <T : Any, R> prepareQueryStatement(
        query: String,
        block: PreparedQueryStatement<T, R>.() -> Unit
    ): R {
        return this.connection.prepareStatement(query).use { preparedStatement ->
            val preparedQueryStatement = PreparedQueryStatement<T, R>(preparedStatement)
            preparedQueryStatement.block()
            preparedQueryStatement.executeQuery()
        }
    }

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
}

class PreparedQueryStatement<T : Any, R>(private val preparedStatement: PreparedStatement) {
    private lateinit var rowMapper: Row.() -> T
    private lateinit var resultMapper: (Sequence<T>) -> R

    fun setParams(block: Params.() -> Unit) {
        Params(preparedStatement).block()
    }

    fun setRowMapper(block: Row.() -> T) {
        rowMapper = block
    }

    fun setResultMapper(block: (result: Sequence<T>) -> R) {
        resultMapper = block
    }

    fun executeQuery(): R {
        val resultSet = preparedStatement.executeQuery()
        return resultSet
            .map { currentResultSet ->
                Row(currentResultSet).rowMapper()
            }
            .let(resultMapper)

    }
}

class PreparedExecuteStatement(private val preparedStatement: PreparedStatement) {
    fun setParams(block: Params.() -> Unit) {
        Params(preparedStatement).block()
    }

    fun execute() {
        preparedStatement.execute()
    }
}

class Params(private val preparedStatement: PreparedStatement) {
    fun setBytes(index: Int, bytes: ByteArray) {
        preparedStatement.setBytes(index, bytes)
    }

    fun setString(index: Int, value: String) {
        preparedStatement.setString(index, value)
    }

    fun setTimestamp(index: Int, localDateTime: LocalDateTime) {
        preparedStatement.setTimestamp(index, Timestamp.valueOf(localDateTime))
    }

    fun setUUID(index: Int, uuid: UUID) {
        preparedStatement.setObject(index, uuid)
    }
}

class Row(private val resultSet: ResultSet) {
    fun getBytes(columnLabel: String): ByteArray {
        return resultSet.getBytes(columnLabel)
    }

    fun getString(columnLabel: String): String {
        return resultSet.getString(columnLabel)
    }
}
