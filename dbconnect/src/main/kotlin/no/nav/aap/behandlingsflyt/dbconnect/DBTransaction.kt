package no.nav.aap.behandlingsflyt.dbconnect

import org.slf4j.LoggerFactory
import java.sql.Connection

internal class DBTransaction(connection: Connection) {
    private val dbConnection: DBConnection = DBConnection(connection)
    private val log = LoggerFactory.getLogger(DBTransaction::class.java)

    internal fun <T> transaction(block: (DBConnection) -> T): T {
        try {
            dbConnection.autoCommitOff()
            val result = block(dbConnection)
            dbConnection.commit()
            return result
        } catch (e: Throwable) {
            log.warn("Kjører rollback etter feil. '{}'", e.message, e)
            dbConnection.rollback()
            throw e
        } finally {
            dbConnection.autoCommitOn()
        }
    }
}
