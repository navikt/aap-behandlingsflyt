package no.nav.aap.behandlingsflyt.dbconnect

import no.nav.aap.behandlingsflyt.dbtest.InitTestDatabase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class DBConnectionTest {

    @BeforeEach
    fun setup() {
        InitTestDatabase.dataSource.transaction { connection ->
            connection.execute("TRUNCATE TEST; ALTER SEQUENCE test_id_seq RESTART WITH 1")
        }
    }

    @Test
    fun `Skriver og henter en rad mot DB`() {
        val result = InitTestDatabase.dataSource.transaction { connection ->
            connection.execute("INSERT INTO test (test) VALUES ('a')")
            connection.queryFirst("SELECT test FROM test") {
                setRowMapper { row -> row.getString("test") }
            }
        }

        assertThat(result).isEqualTo("a")
    }

    @Test
    fun `Skriver og henter to rader mot DB`() {
        val result = InitTestDatabase.dataSource.transaction { connection ->
            connection.execute("INSERT INTO test (test) VALUES ('a'), ('b')")
            connection.queryList("SELECT test FROM test") {
                setRowMapper { row -> row.getString("test") }
            }
        }

        assertThat(result)
            .hasSize(2)
            .contains("a", "b")
    }

    @Test
    fun `Henter ingen rader fra DB`() {
        val result = InitTestDatabase.dataSource.transaction { connection ->
            connection.queryFirstOrNull("SELECT test FROM test") {
                setRowMapper { row -> row.getString("test") }
            }
        }

        assertThat(result).isNull()
    }

    @Test
    fun `Skriver og henter key og verdier fra DB`() {
        val (result, key) = InitTestDatabase.dataSource.transaction { connection ->
            connection.execute("INSERT INTO test (test) VALUES ('a')")
            val key = connection.executeReturnKey("INSERT INTO test (test) VALUES ('b')")
            connection.queryList("SELECT test FROM test") {
                setRowMapper { row -> row.getString("test") }
            } to key
        }

        assertThat(result)
            .hasSize(2)
            .contains("a", "b")
        assertThat(key).isEqualTo(2)
    }

    @Test
    fun `Skriver og henter keys og verdier fra DB`() {
        val (result, keys) = InitTestDatabase.dataSource.transaction { connection ->
            connection.execute("INSERT INTO test (test) VALUES ('a'), ('b')")
            val keys = connection.executeReturnKeys("INSERT INTO test (test) VALUES ('c'), ('d')")
            connection.queryList("SELECT test FROM test") {
                setRowMapper { row -> row.getString("test") }
            } to keys
        }

        assertThat(result)
            .hasSize(4)
            .contains("a", "b", "c", "d")
        assertThat(keys)
            .hasSize(2)
            .contains(3, 4)
    }

    @Test
    fun `Henter tomt resultat fra DB`() {
        val result = InitTestDatabase.dataSource.transaction { connection ->
            connection.queryList("SELECT test FROM test") {
                setRowMapper { row -> row.getString("test") }
            }
        }

        assertThat(result).isEmpty()
    }
}
