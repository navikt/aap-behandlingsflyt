package no.nav.aap.behandlingsflyt.dbtest

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.testcontainers.containers.PostgreSQLContainer
import javax.sql.DataSource

object InitTestDatabase {
    val dataSource: DataSource

    init {
        // Postgres 15 korresponderer til versjon i nais.yaml
        val postgres = PostgreSQLContainer<Nothing>("postgres:15")
        postgres.start()
        val jdbcUrl = postgres.jdbcUrl
        val username = postgres.username
        val password = postgres.password
        dataSource = HikariDataSource(HikariConfig().apply {
            this.jdbcUrl = jdbcUrl
            this.username = username
            this.password = password
            minimumIdle = 1
            initializationFailTimeout = 30000
            idleTimeout = 10000
            connectionTimeout = 10000
            maxLifetime = 900000
            connectionTestQuery = "SELECT 1"
        })

        Flyway
            .configure()
            .cleanDisabled(false)
            .cleanOnValidationError(true)
            .dataSource(dataSource)
            .locations("flyway")
            .validateMigrationNaming(true)
            .load()
            .migrate()
    }
}
