package no.nav.aap.behandlingsflyt.dbconnect

import javax.sql.DataSource

fun <T> DataSource.transaction(block: (DBConnection) -> T): T {
    return this.connection.use { connection ->
        val dbTransaction = DBTransaction<T>(connection)
        dbTransaction.transaction(block)
    }
}
