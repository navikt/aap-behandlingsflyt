package no.nav.aap.behandlingsflyt.dbconnect

import no.nav.aap.verdityper.Periode
import java.math.BigDecimal
import java.sql.Date
import java.sql.PreparedStatement
import java.sql.Timestamp
import java.sql.Types
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class Params(private val preparedStatement: PreparedStatement) {
    fun setBytes(index: Int, bytes: ByteArray?) {
        preparedStatement.setBytes(index, bytes)
    }

    fun setString(index: Int, value: String?) {
        preparedStatement.setString(index, value)
    }

    fun setEnumName(index: Int, value: Enum<*>?) {
        preparedStatement.setString(index, value?.name)
    }

    fun setInt(index: Int, value: Int?) {
        if (value == null) {
            preparedStatement.setNull(index, Types.SMALLINT)
        } else {
            preparedStatement.setInt(index, value)
        }
    }

    fun setLong(index: Int, value: Long?) {
        if (value == null) {
            preparedStatement.setNull(index, Types.NUMERIC)
        } else {
            preparedStatement.setLong(index, value)
        }
    }

    fun setBigDecimal(index: Int, value: BigDecimal?) {
        if (value == null) {
            preparedStatement.setNull(index, Types.NUMERIC)
        } else {
            preparedStatement.setBigDecimal(index, value)
        }
    }

    fun setUUID(index: Int, uuid: UUID?) {
        preparedStatement.setObject(index, uuid)
    }

    fun setBoolean(index: Int, value: Boolean?) {
        if (value == null) {
            preparedStatement.setNull(index, Types.BOOLEAN)
        } else {
            preparedStatement.setBoolean(index, value)
        }
    }

    fun setPeriode(index: Int, periode: Periode?) {
        preparedStatement.setString(index, periode?.let(DaterangeParser::toSQL))
    }

    fun setLocalDate(index: Int, localDate: LocalDate?) {
        preparedStatement.setDate(index, localDate?.let(Date::valueOf))
    }

    fun setLocalDateTime(index: Int, localDateTime: LocalDateTime?) {
        preparedStatement.setTimestamp(index, localDateTime?.let(Timestamp::valueOf))
    }
}
