package no.nav.aap.behandlingsflyt.dbconnect

import javax.sql.DataSource

fun <T> DataSource.transaction(readOnly: Boolean = false, block: (DBConnection) -> T): T {
    return this.connection.use { connection ->
        if (readOnly) {
            connection.isReadOnly = true // Setter transaction i read-only
        }
        val dbTransaction = DBTransaction(connection)
        dbTransaction.transaction(block)
    }
}
