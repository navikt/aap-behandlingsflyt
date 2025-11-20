package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.ÅrsakTilSettPåVent
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.help.sak
import no.nav.aap.behandlingsflyt.hendelse.mottak.BehandlingSattPåVent
import no.nav.aap.behandlingsflyt.integrasjon.createGatewayProvider
import no.nav.aap.behandlingsflyt.prosessering.VarsleOppgaveOmHendelseJobbUtFører
import no.nav.aap.behandlingsflyt.repository.postgresRepositoryRegistry
import no.nav.aap.behandlingsflyt.test.FakeUnleash
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.komponenter.verdityper.Bruker
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Condition
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.time.LocalDate

class AvklaringsbehovOrkestratorTest {
    companion object {
        private lateinit var dataSource: TestDataSource

        @BeforeAll
        @JvmStatic
        fun setup() {
            dataSource = TestDataSource()
        }

        @AfterAll
        @JvmStatic
        fun tearDown() = dataSource.close()
    }

    @Test
    fun `behandlingHendelseService dot stoppet blir kalt når en behandling er satt på vent`() {
        val uthentedeJobber = dataSource.transaction { connection ->
            val avklaringsbehovOrkestrator = AvklaringsbehovOrkestrator(
                postgresRepositoryRegistry.provider(connection),
                createGatewayProvider { register<FakeUnleash>() },
            )
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)

            // Act
            avklaringsbehovOrkestrator.settBehandlingPåVent(
                behandling.id,
                BehandlingSattPåVent(
                    frist = LocalDate.now().plusDays(1),
                    begrunnelse = "en god begrunnelse",
                    behandlingVersjon = behandling.versjon,
                    bruker = Bruker("123"),
                    grunn = ÅrsakTilSettPåVent.VENTER_PÅ_MEDISINSKE_OPPLYSNINGER,
                )
            )

            hentJobber(connection)
        }
        assertThat(uthentedeJobber).haveAtLeastOne(
            Condition(
                { it == VarsleOppgaveOmHendelseJobbUtFører.type },
                "skal være av rett type"
            )
        )
    }

    private fun hentJobber(connection: DBConnection): List<String> {
        return connection.queryList(
            """
            SELECT type FROM JOBB
        """.trimIndent()
        ) {
            setRowMapper {
                it.getString("type")
            }
        }
    }
}