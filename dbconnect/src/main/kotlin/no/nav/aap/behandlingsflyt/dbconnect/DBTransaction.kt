package no.nav.aap.behandlingsflyt.dbconnect

import java.sql.Connection

internal class DBTransaction(connection: Connection) {
    private val dbConnection: DBConnection = DBConnection(connection)

    internal fun <T> transaction(block: (DBConnection) -> T): T {
        try {
            dbConnection.autoCommitOff()
            val result = block(dbConnection)
            dbConnection.commit()
            return result
        } catch (e: Throwable) {
            dbConnection.rollback()
            throw e
        } finally {
            dbConnection.autoCommitOn()
        }
    }
}
