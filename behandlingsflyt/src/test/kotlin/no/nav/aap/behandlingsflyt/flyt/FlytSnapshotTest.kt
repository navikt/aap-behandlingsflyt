package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.RepetitionInfo
import org.junit.jupiter.params.Parameter
import org.junit.jupiter.params.ParameterizedClass
import org.junit.jupiter.params.provider.MethodSource
import kotlin.reflect.KClass

@ParameterizedClass
@MethodSource("unleashTestDataSource")
class FlytSnapshotTest : AbstraktFlytOrkestratorSnapshotTest() {

    @field:Parameter
    lateinit var unleashGateway: KClass<out UnleashGateway>

    override fun unleashGateway() = unleashGateway

    @BeforeAll
    fun settOppDataSomSkalTømmesFørSnapshot() {
        dataSource.connection.use { connection ->
            connection.autoCommit = true
            connection.createStatement().use { statement ->
                statement.execute("CREATE TABLE snapshot_sekvens (id BIGINT GENERATED ALWAYS AS IDENTITY)")
                statement.execute("INSERT INTO snapshot_sekvens DEFAULT VALUES")
            }
        }
    }

    @RepeatedTest(2)
    fun `tomt snapshot isolerer tabeller og sekvenser mellom tester og togglevarianter`(repetition: RepetitionInfo) {
        assertThat(gatewayProvider.provide<UnleashGateway>()).isExactlyInstanceOf(unleashGateway.java)

        dataSource.connection.use { connection ->
            connection.autoCommit = true
            connection.createStatement().use { statement ->
                statement.executeQuery("INSERT INTO snapshot_sekvens DEFAULT VALUES RETURNING id").use { result ->
                    assertThat(result.next()).isTrue()
                    assertThat(result.getLong("id")).isEqualTo(1L)
                }
                statement.execute(
                    "CREATE TABLE snapshot_isolasjon (id BIGINT GENERATED ALWAYS AS IDENTITY, verdi INTEGER NOT NULL)"
                )
            }
            connection.prepareStatement("INSERT INTO snapshot_isolasjon (verdi) VALUES (?) RETURNING id, verdi")
                .use { statement ->
                    statement.setInt(1, repetition.currentRepetition)
                    statement.executeQuery().use { result ->
                        assertThat(result.next()).isTrue()
                        assertThat(result.getLong("id")).isEqualTo(1L)
                        assertThat(result.getInt("verdi")).isEqualTo(repetition.currentRepetition)
                    }
                }
        }
    }
}
