package no.nav.aap.behandlingsflyt.dbconnect

import no.nav.aap.behandlingsflyt.dbtest.InitTestDatabase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class DBTransactionTest {

    @BeforeEach
    fun setup() {
        InitTestDatabase.dataSource.transaction { connection ->
            connection.execute("TRUNCATE TEST_TRANSACTION")
        }
    }

    @Test
    fun `Skriver og henter en rad mot DB`() {
        assertThrows<IllegalStateException> {
            InitTestDatabase.dataSource.transaction { connection ->
                connection.execute("INSERT INTO TEST_TRANSACTION (TEST) VALUES ('a')")
                connection.markerSavepoint()
                connection.execute("INSERT INTO TEST_TRANSACTION (TEST) VALUES ('b')")
                error("error")
            }
        }
        assertThrows<IllegalStateException> {
            InitTestDatabase.dataSource.transaction { connection ->
                connection.execute("INSERT INTO TEST_TRANSACTION (TEST) VALUES ('c')")
                error("error")
            }
        }
        InitTestDatabase.dataSource.transaction { connection ->
            connection.execute("INSERT INTO TEST_TRANSACTION (TEST) VALUES ('d')")
        }

        val result = InitTestDatabase.dataSource.transaction { connection ->
            connection.queryList("SELECT TEST FROM TEST_TRANSACTION") {
                setRowMapper { row -> row.getString("TEST") }
            }
        }

        assertThat(result)
            .hasSize(2)
            .containsExactly("a", "d")
    }
}
