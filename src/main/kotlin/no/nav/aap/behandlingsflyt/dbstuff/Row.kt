package no.nav.aap.behandlingsflyt.dbstuff

import no.nav.aap.behandlingsflyt.Periode
import java.sql.ResultSet
import java.time.LocalDateTime
import java.util.*

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

    fun getBoolean(columnLabel: String): Boolean {
        return resultSet.getBoolean(columnLabel)
    }

    fun getLocalDateTime(columnLabel: String): LocalDateTime {
        return resultSet.getTimestamp(columnLabel).toLocalDateTime()
    }
}
