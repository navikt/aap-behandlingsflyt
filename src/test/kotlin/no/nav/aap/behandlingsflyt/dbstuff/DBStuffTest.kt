package no.nav.aap.behandlingsflyt.dbstuff

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test;

internal class DBStuffTest : DatabaseTestBase() {

    @Test
    fun `Skriver og henter en rad mot DB`() {
        val result = InitTestDatabase.dataSource.transaction { connection ->
            connection.prepareExecuteStatement("INSERT INTO test (test) VALUES ('1')") {}
            connection.prepareQueryStatement("SELECT test FROM test") {
                setRowMapper { row -> row.getString("test") }
                setResultMapper { it.first() }
            }
        }

        assertThat(result).isEqualTo("1")
    }

    @Test
    fun `Skriver og henter keys og verdier fra DB`() {
        val (result, keys) = InitTestDatabase.dataSource.transaction { connection ->
            connection.prepareExecuteStatement("INSERT INTO test (test) VALUES ('a'), ('b')") {}
            val keys =
                connection.prepareExecuteStatementReturnAutoGenKeys("INSERT INTO test (test) VALUES ('c'), ('d')") {}
            connection.prepareQueryStatement("SELECT test FROM test") {
                setRowMapper { row -> row.getString("test") }
                setResultMapper { it.toList() }
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
            connection.prepareQueryStatement("SELECT test FROM test") {
                setRowMapper { row -> row.getString("test") }
                setResultMapper(Sequence<String>::toList)
            }
        }

        assertThat(result).isEmpty()
    }

    @Test
    fun `ResultSetIterator må svare false på hasNext hvis den forsøkes å itereres over flere ganger`() {
        val result = InitTestDatabase.dataSource.transaction { connection ->
            connection.prepareExecuteStatement("INSERT INTO test (test) VALUES ('a'), ('b'), ('c'), ('d')") {}
            connection.prepareQueryStatement("SELECT test FROM test") {
                setRowMapper { row -> row.getString("test") }
                setResultMapper {
                    val iterator = it.iterator()
                    // To kall på samme iterator skal ikke føre til exception
                    iterator.asSequence().toList() // Radene blir hentet ut her
                    iterator.asSequence().toList()// Denne lista blir tom, siden radene allerede er lest
                }
            }
        }

        assertThat(result).isEmpty()
    }
}
