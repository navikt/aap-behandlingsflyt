package no.nav.aap.behandlingsflyt.dbconnect

import no.nav.aap.behandlingsflyt.dbtest.InitTestDatabase
import no.nav.aap.verdityper.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month
import java.util.*

internal class ParamsOgRowTest {

    @BeforeEach
    fun setup() {
        InitTestDatabase.dataSource.transaction { connection ->
            connection.execute("TRUNCATE TEST_BYTES, TEST_STRING, TEST_ENUM, TEST_INT, TEST_LONG, TEST_BIG_DECIMAL, TEST_UUID, TEST_BOOLEAN, TEST_DATERANGE, TEST_LOCALDATE, TEST_LOCALDATETIME")
        }
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

    enum class TestEnum {
        TEST
    }

    @Test
    fun `Skriver og leser Enum og null-verdi riktig`() {
        InitTestDatabase.dataSource.transaction { connection ->
            connection.execute(
                """
                    INSERT INTO TEST_ENUM (TEST, TEST_NULL)
                    VALUES (?, ?)
                """.trimMargin()
            ) {
                setParams {
                    setEnumName(1, TestEnum.TEST)
                    setEnumName(2, null)
                }
            }
            connection.queryFirst("SELECT * FROM TEST_ENUM") {
                setRowMapper { row ->
                    assertThat(row.getEnumOrNull<TestEnum?, TestEnum>("TEST")).isEqualTo(TestEnum.TEST)
                    assertThat(row.getEnum<TestEnum>("TEST")).isEqualTo(TestEnum.TEST)
                    assertThat(row.getEnumOrNull<TestEnum?, TestEnum>("TEST_NULL")).isNull()
                    assertThrows<IllegalArgumentException> { row.getEnum<TestEnum>("TEST_NULL") }
                }
            }
        }
    }

    @Test
    fun `Skriver og leser Int og null-verdi riktig`() {
        InitTestDatabase.dataSource.transaction { connection ->
            connection.execute(
                """
                    INSERT INTO TEST_INT (TEST_0, TEST_1, TEST_NULL)
                    VALUES (?, ?, ?)
                """.trimMargin()
            ) {
                setParams {
                    setInt(1, 0)
                    setInt(2, 1)
                    setInt(3, null)
                }
            }
            connection.queryFirst("SELECT * FROM TEST_INT") {
                setRowMapper { row ->
                    assertThat(row.getIntOrNull("TEST_0")).isEqualTo(0)
                    assertThat(row.getInt("TEST_0")).isEqualTo(0)
                    assertThat(row.getIntOrNull("TEST_1")).isEqualTo(1)
                    assertThat(row.getInt("TEST_1")).isEqualTo(1)
                    assertThat(row.getIntOrNull("TEST_NULL")).isNull()
                    assertThrows<IllegalArgumentException> { row.getInt("TEST_NULL") }
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
    fun `Skriver og leser BigDecimal og null-verdi riktig`() {
        InitTestDatabase.dataSource.transaction { connection ->
            connection.execute(
                """
                    INSERT INTO TEST_BIG_DECIMAL (TEST_0, TEST_1, TEST_NULL)
                    VALUES (?, ?, ?)
                """.trimMargin()
            ) {
                setParams {
                    setBigDecimal(1, BigDecimal.ZERO)
                    setBigDecimal(2, BigDecimal("12.34"))
                    setBigDecimal(3, null)
                }
            }
            connection.queryFirst("SELECT * FROM TEST_BIG_DECIMAL") {
                setRowMapper { row ->
                    // Er lik 0.00 fordi kolonnen i tabellen precision = 4 og scale = 2
                    assertThat(row.getBigDecimalOrNull("TEST_0")).isEqualTo("0.00")
                    assertThat(row.getBigDecimal("TEST_0")).isEqualTo("0.00")
                    assertThat(row.getBigDecimalOrNull("TEST_1")).isEqualTo(BigDecimal("12.34"))
                    assertThat(row.getBigDecimal("TEST_1")).isEqualTo(BigDecimal("12.34"))
                    assertThat(row.getBigDecimalOrNull("TEST_NULL")).isNull()
                    assertThrows<IllegalArgumentException> { row.getBigDecimal("TEST_NULL") }
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
                    assertThat(row.getPeriodeOrNull("TEST"))
                        .isEqualTo(Periode(LocalDate.now(), LocalDate.now()))
                    assertThat(row.getPeriode("TEST")).isEqualTo(Periode(LocalDate.now(), LocalDate.now()))
                    assertThat(row.getPeriodeOrNull("TEST_NULL")).isNull()
                    assertThrows<IllegalArgumentException> { row.getPeriode("TEST_NULL") }
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