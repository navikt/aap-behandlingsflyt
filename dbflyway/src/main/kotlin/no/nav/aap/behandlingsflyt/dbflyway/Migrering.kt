package no.nav.aap.behandlingsflyt.dbflyway

import org.flywaydb.core.Flyway
import javax.sql.DataSource

object Migrering {
    fun migrate(dataSource: DataSource) {
        val flyway = Flyway
            .configure()
            .cleanDisabled(!cleanDb())
            .cleanOnValidationError(cleanDb())
            .dataSource(dataSource)
            .locations("flyway")
            .validateMigrationNaming(true)
            .load()

        flyway.migrate()
    }

    //FIXME: Før prodsetting, slett migreringsscriptet V1.0__Ikke_for_prod.sql
    private fun cleanDb(): Boolean {
        return Miljø.er() in arrayOf(MiljøKode.LOKALT, MiljøKode.DEV)
    }
}
