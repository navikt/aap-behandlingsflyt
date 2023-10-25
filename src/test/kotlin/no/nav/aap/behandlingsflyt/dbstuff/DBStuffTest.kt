package no.nav.aap.behandlingsflyt.dbstuff

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test;

internal class DBStuffTest : DatabaseTestBase() {

    @Test
    fun `Test test`() {
        val result = InitTestDatabase.dataSource.transaction { connection ->
            connection.prepareExecuteStatement("INSERT INTO test VALUES ('1')") {}
            connection.prepareQueryStatement("SELECT test FROM test") {
                setRowMapper { getString("test") }
                setResultMapper { it.first() }
            }
        }

        assertThat(result).isEqualTo("1")
    }
}
