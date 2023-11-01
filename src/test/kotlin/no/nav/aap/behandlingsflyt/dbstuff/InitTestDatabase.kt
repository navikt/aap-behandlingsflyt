package no.nav.aap.behandlingsflyt.dbstuff

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.BeforeEach
import org.testcontainers.containers.PostgreSQLContainer
import javax.sql.DataSource

internal object InitTestDatabase {
    internal val dataSource: DataSource

    init {
        var password = "postgres"
        var username = "postgres"
        var jdbcUrl = "jdbc:postgresql://localhost:5432/public"
        if (System.getenv("GITHUB_ACTIONS") != "true") {
            val postgres = PostgreSQLContainer<Nothing>("postgres:16")
            postgres.start()
            jdbcUrl = postgres.jdbcUrl
            username = postgres.username
            password = postgres.password
        }
        dataSource = HikariDataSource(HikariConfig().apply {
            this.jdbcUrl = jdbcUrl
            this.username = username
            this.password = password
            minimumIdle = 1
            maximumPoolSize = 3
            initializationFailTimeout = 30000
            idleTimeout = 10000
            connectionTimeout = 50000
            maxLifetime = 900000
            connectionTestQuery = "SELECT 1"
        })

        Flyway
            .configure()
            .dataSource(dataSource)
            .locations("flyway")
            .load()
            .migrate()
    }
}

internal abstract class DatabaseTestBase {
    @BeforeEach
    fun clearTables() {
        InitTestDatabase.dataSource.transaction { connection ->
            connection.execute("TRUNCATE TEST, TEST_BYTES, TEST_STRING, TEST_LONG, TEST_UUID, TEST_DATERANGE, TEST_BOOLEAN, TEST_LOCALDATETIME; ALTER SEQUENCE test_id_seq RESTART WITH 1")
        }
    }
}
