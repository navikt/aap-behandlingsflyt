package no.nav.aap.behandlingsflyt.dbstuff

import no.nav.aap.behandlingsflyt.Periode
import java.sql.PreparedStatement
import java.sql.Timestamp
import java.time.LocalDateTime
import java.util.*

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
