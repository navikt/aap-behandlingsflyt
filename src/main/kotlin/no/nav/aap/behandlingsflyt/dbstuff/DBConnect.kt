package no.nav.aap.behandlingsflyt.dbstuff

import javax.sql.DataSource

fun <T> DataSource.connect(block: (DBConnection) -> T): T {
    return this.connection.use { connection ->
        block(DBConnection(connection))
    }
}

fun <T> DataSource.transaction(block: (DBConnection) -> T): T {
    return this.connection.use { connection ->
        val dbConnection = DBConnection(connection)
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
