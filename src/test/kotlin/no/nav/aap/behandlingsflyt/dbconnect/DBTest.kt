package no.nav.aap.behandlingsflyt.dbconnect

import no.nav.aap.behandlingsflyt.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month
import java.util.*

internal class DBTest {

    @BeforeEach
    fun setup() {
        InitTestDatabase.dataSource.transaction { connection ->
            connection.execute("TRUNCATE TEST, TEST_BYTES, TEST_STRING, TEST_LONG, TEST_UUID, TEST_DATERANGE, TEST_BOOLEAN, TEST_LOCALDATETIME; ALTER SEQUENCE test_id_seq RESTART WITH 1")
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

    @Test
    fun `Skriver og leser ByteArray og null-verdi riktig`() {
        InitTestDatabase.dataSource.transaction { connection ->
            connection.execute(
                """
                    INSERT INTO TEST_BYTES (TEST, TEST_NULL)
                    VALUES (?, ?)
                """.trimMargin()
            ) {
                setParams {
                    setBytes(1, "test".toByteArray())
                    setBytes(2, null)
                }
            }
            connection.queryFirst("SELECT * FROM TEST_BYTES") {
                setRowMapper { row ->
                    assertThat(row.getBytesOrNull("TEST")).asString().isEqualTo("test")
                    assertThat(row.getBytes("TEST")).asString().isEqualTo("test")
                    assertThat(row.getBytesOrNull("TEST_NULL")).isNull()
                    assertThrows<IllegalArgumentException> { row.getBytes("TEST_NULL") }
                }
            }
        }
    }

    @Test
    fun `Skriver og leser String og null-verdi riktig`() {
        InitTestDatabase.dataSource.transaction { connection ->
            connection.execute(
                """
                    INSERT INTO TEST_STRING (TEST, TEST_NULL)
                    VALUES (?, ?)
                """.trimMargin()
            ) {
                setParams {
                    setString(1, "test")
                    setString(2, null)
                }
            }
            connection.queryFirst("SELECT * FROM TEST_STRING") {
                setRowMapper { row ->
                    assertThat(row.getStringOrNull("TEST")).isEqualTo("test")
                    assertThat(row.getString("TEST")).isEqualTo("test")
                    assertThat(row.getStringOrNull("TEST_NULL")).isNull()
                    assertThrows<IllegalArgumentException> { row.getString("TEST_NULL") }
                }
            }
        }
    }

    @Test
    fun `Skriver og leser Long og null-verdi riktig`() {
        InitTestDatabase.dataSource.transaction { connection ->
            connection.execute(
                """
                    INSERT INTO TEST_LONG (TEST_0, TEST_1, TEST_NULL)
                    VALUES (?, ?, ?)
                """.trimMargin()
            ) {
                setParams {
                    setLong(1, 0)
                    setLong(2, 1)
                    setLong(3, null)
                }
            }
            connection.queryFirst("SELECT * FROM TEST_LONG") {
                setRowMapper { row ->
                    assertThat(row.getLongOrNull("TEST_0")).isEqualTo(0)
                    assertThat(row.getLong("TEST_0")).isEqualTo(0)
                    assertThat(row.getLongOrNull("TEST_1")).isEqualTo(1)
                    assertThat(row.getLong("TEST_1")).isEqualTo(1)
                    assertThat(row.getLongOrNull("TEST_NULL")).isNull()
                    assertThrows<IllegalArgumentException> { row.getLong("TEST_NULL") }
                }
            }
        }
    }

    @Test
    fun `Skriver og leser UUID og null-verdi riktig`() {
        val randomUUID = UUID.randomUUID()
        InitTestDatabase.dataSource.transaction { connection ->
            connection.execute(
                """
                    INSERT INTO TEST_UUID (TEST, TEST_NULL)
                    VALUES (?, ?)
                """.trimMargin()
            ) {
                setParams {
                    setUUID(1, randomUUID)
                    setUUID(2, null)
                }
            }
            connection.queryFirst("SELECT * FROM TEST_UUID") {
                setRowMapper { row ->
                    assertThat(row.getUUIDOrNull("TEST")).isEqualTo(randomUUID)
                    assertThat(row.getUUID("TEST")).isEqualTo(randomUUID)
                    assertThat(row.getUUIDOrNull("TEST_NULL")).isNull()
                    assertThrows<IllegalArgumentException> { row.getUUID("TEST_NULL") }
                }
            }
        }
    }

    @Test
    fun `Skriver og leser Periode og null-verdi riktig`() {
        InitTestDatabase.dataSource.transaction { connection ->
            connection.execute(
                """
                    INSERT INTO TEST_DATERANGE (TEST, TEST_NULL)
                    VALUES (?::daterange, ?::daterange)
                """.trimMargin()
            ) {
                setParams {
                    setPeriode(1, Periode(LocalDate.now(), LocalDate.now()))
                    setPeriode(2, null)
                }
            }
            connection.queryFirst("SELECT * FROM TEST_DATERANGE") {
                setRowMapper { row ->
                    assertThat(row.getPeriodeOrNull("TEST")).isEqualTo(Periode(LocalDate.now(), LocalDate.now()))
                    assertThat(row.getPeriode("TEST")).isEqualTo(Periode(LocalDate.now(), LocalDate.now()))
                    assertThat(row.getPeriodeOrNull("TEST_NULL")).isNull()
                    assertThrows<IllegalArgumentException> { row.getPeriode("TEST_NULL") }
                }
            }
        }
    }

    @Test
    fun `Skriver og leser Boolean og null-verdi riktig`() {
        InitTestDatabase.dataSource.transaction { connection ->
            connection.execute(
                """
                    INSERT INTO TEST_BOOLEAN (TEST_FALSE, TEST_TRUE, TEST_NULL)
                    VALUES (?, ?, ?)
                """.trimMargin()
            ) {
                setParams {
                    setBoolean(1, false)
                    setBoolean(2, true)
                    setBoolean(3, null)
                }
            }
            connection.queryFirst("SELECT * FROM TEST_BOOLEAN") {
                setRowMapper { row ->
                    assertThat(row.getBooleanOrNull("TEST_FALSE")).isFalse
                    assertThat(row.getBoolean("TEST_FALSE")).isFalse
                    assertThat(row.getBooleanOrNull("TEST_TRUE")).isTrue
                    assertThat(row.getBoolean("TEST_TRUE")).isTrue
                    assertThat(row.getBooleanOrNull("TEST_NULL")).isNull()
                    assertThrows<IllegalArgumentException> { row.getBoolean("TEST_NULL") }
                }
            }
        }
    }

    @Test
    fun `Skriver og leser LocalDate og null-verdi riktig`() {
        val localDate = LocalDate.of(2016, Month.AUGUST, 12)
        InitTestDatabase.dataSource.transaction { connection ->
            connection.execute(
                """
                    INSERT INTO TEST_LOCALDATE (TEST, TEST_NULL)
                    VALUES (?, ?)
                """.trimMargin()
            ) {
                setParams {
                    setLocalDate(1, localDate)
                    setLocalDate(2, null)
                }
            }
            connection.queryFirst("SELECT * FROM TEST_LOCALDATE") {
                setRowMapper { row ->
                    assertThat(row.getLocalDateOrNull("TEST")).isEqualTo(localDate)
                    assertThat(row.getLocalDate("TEST")).isEqualTo(localDate)
                    assertThat(row.getLocalDateOrNull("TEST_NULL")).isNull()
                    assertThrows<IllegalArgumentException> { row.getLocalDate("TEST_NULL") }
                }
            }
        }
    }

    @Test
    fun `Skriver og leser LocalDateTime og null-verdi riktig`() {
        val localDateTime = LocalDateTime.of(2016, Month.AUGUST, 12, 9, 38, 12, 123456000)
        InitTestDatabase.dataSource.transaction { connection ->
            connection.execute(
                """
                    INSERT INTO TEST_LOCALDATETIME (TEST, TEST_NULL)
                    VALUES (?, ?)
                """.trimMargin()
            ) {
                setParams {
                    setLocalDateTime(1, localDateTime)
                    setLocalDateTime(2, null)
                }
            }
            connection.queryFirst("SELECT * FROM TEST_LOCALDATETIME") {
                setRowMapper { row ->
                    assertThat(row.getLocalDateTimeOrNull("TEST")).isEqualTo(localDateTime)
                    assertThat(row.getLocalDateTime("TEST")).isEqualTo(localDateTime)
                    assertThat(row.getLocalDateTimeOrNull("TEST_NULL")).isNull()
                    assertThrows<IllegalArgumentException> { row.getLocalDateTime("TEST_NULL") }
                }
            }
        }
    }
}
