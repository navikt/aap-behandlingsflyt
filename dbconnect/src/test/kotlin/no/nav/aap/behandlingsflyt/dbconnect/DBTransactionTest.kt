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
    fun `Kan skrive til DB i en transaksjon`() {
        InitTestDatabase.dataSource.transaction { connection ->
            connection.execute("INSERT INTO TEST_TRANSACTION (TEST) VALUES ('d')")
        }

        val result = InitTestDatabase.dataSource.transaction { connection ->
            connection.queryList("SELECT TEST FROM TEST_TRANSACTION") {
                setRowMapper { row -> row.getString("TEST") }
            }
        }

        assertThat(result)
            .hasSize(1)
            .containsExactly("d")
    }

    @Test
    fun `Feil i en transaksjon fører til en rollback`() {
        assertThrows<IllegalStateException> {
            InitTestDatabase.dataSource.transaction { connection ->
                connection.execute("INSERT INTO TEST_TRANSACTION (TEST) VALUES ('c')")
                error("error")
            }
        }

        val result = InitTestDatabase.dataSource.transaction { connection ->
            connection.queryList("SELECT TEST FROM TEST_TRANSACTION") {
                setRowMapper { row -> row.getString("TEST") }
            }
        }

        assertThat(result).isEmpty()
    }

    @Test
    fun `Feil i en transaksjon med savepoint fører til en rollback tilbake til siste savepoint`() {
        assertThrows<IllegalStateException> {
            InitTestDatabase.dataSource.transaction { connection ->
                connection.execute("INSERT INTO TEST_TRANSACTION (TEST) VALUES ('a')")
                connection.markerSavepoint()
                connection.execute("INSERT INTO TEST_TRANSACTION (TEST) VALUES ('b')")
                connection.markerSavepoint()
                connection.execute("INSERT INTO TEST_TRANSACTION (TEST) VALUES ('c')")
                error("error")
            }
        }

        val result = InitTestDatabase.dataSource.transaction { connection ->
            connection.queryList("SELECT TEST FROM TEST_TRANSACTION") {
                setRowMapper { row -> row.getString("TEST") }
            }
        }

        assertThat(result)
            .hasSize(2)
            .containsExactly("a", "b")
    }
}
