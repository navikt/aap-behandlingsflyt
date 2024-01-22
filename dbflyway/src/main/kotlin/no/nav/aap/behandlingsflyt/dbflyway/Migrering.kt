package no.nav.aap.behandlingsflyt.dbflyway

import org.flywaydb.core.Flyway
import javax.sql.DataSource

object Migrering {
    fun migrate(dataSource: DataSource) {
        val flyway = Flyway
            .configure()
            .cleanDisabled(false) // TODO: husk å skru av denne før prod
            .cleanOnValidationError(true) // TODO: husk å skru av denne før prod
            .dataSource(dataSource)
            .locations("flyway")
            .validateMigrationNaming(true)
            .load()

        flyway.migrate()
    }

}