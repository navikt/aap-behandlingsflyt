package no.nav.aap.behandlingsflyt.dbstuff

import no.nav.aap.behandlingsflyt.Periode
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Savepoint
import java.sql.Statement
import java.sql.Timestamp
import java.time.LocalDateTime
import java.util.*
import javax.sql.DataSource


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

fun <T : Any> ResultSet.map(block: (rs: ResultSet) -> T): Sequence<T> {
    return ResultSetSequence(this).map(block)
}

fun <T> DataSource.connect(block: (DbConnection) -> T): T {
    return this.connection.use { connection ->
        block(DbConnection(connection))
    }
}

fun <T> DataSource.transaction(block: (DbConnection) -> T): T {
    return this.connection.use { connection ->
        val dbConnection = DbConnection(connection)
        try {
            connection.autoCommit = false
            val result = block(dbConnection)
            connection.commit()
            result
        } catch (e: Throwable) {
            dbConnection.rollback()
            throw e
        } finally {
            connection.autoCommit = true
        }
    }
}

class DbConnection(private val connection: Connection) {
    private var savepoint: Savepoint? = null

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

class PreparedQueryStatement<T : Any, R>(private val preparedStatement: PreparedStatement) {
    private lateinit var rowMapper: (Row) -> T
    private lateinit var resultMapper: (Sequence<T>) -> R

    fun setParams(block: Params.() -> Unit) {
        Params(preparedStatement).block()
    }

    fun setRowMapper(block: (Row) -> T) {
        rowMapper = block
    }

    fun setResultMapper(block: (result: Sequence<T>) -> R) {
        resultMapper = block
    }

    fun executeQuery(): R {
        val resultSet = preparedStatement.executeQuery()
        return resultSet
            .map { currentResultSet ->
                rowMapper(Row(currentResultSet))
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

    fun setDateRange(index: Int, periode: Periode) {
        preparedStatement.setString(index, DaterangeParser.toSQL(periode))
    }

    fun setLong(index: Int, value: Long) {
        preparedStatement.setLong(index, value)
    }
}

class Row(private val resultSet: ResultSet) {
    fun getBytes(columnLabel: String): ByteArray {
        return resultSet.getBytes(columnLabel)
    }

    fun getString(columnLabel: String): String {
        return resultSet.getString(columnLabel)
    }

    fun getLong(columnLabel: String): Long {
        return resultSet.getLong(columnLabel)
    }

    fun getUUID(columnLabel: String): UUID {
        return UUID.fromString(resultSet.getString(columnLabel))
    }

    fun getDateRange(columnLabel: String): Periode {
        val dateRange = resultSet.getString(columnLabel)
        return DaterangeParser.fromSQL(dateRange)
    }
}
