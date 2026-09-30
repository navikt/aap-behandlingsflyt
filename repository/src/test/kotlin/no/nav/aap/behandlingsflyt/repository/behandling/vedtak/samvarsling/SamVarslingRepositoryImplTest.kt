package no.nav.aap.behandlingsflyt.repository.behandling.vedtak.samvarsling

import no.nav.aap.behandlingsflyt.datadeling.sam.SamVarsling
import no.nav.aap.behandlingsflyt.datadeling.sam.SamordneVedtakRequest
import no.nav.aap.behandlingsflyt.datadeling.sam.SamordneVedtakRespons
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.help.opprettSak
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.time.LocalDate

class SamVarslingRepositoryImplTest {
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
    fun `Kan skrive og lese varslinger`() {
        dataSource.transaction { connection ->
            val repo = SamVarslingRepositoryImpl(connection)
            val sak = opprettSak(connection, 1 januar 2020)
            val behandling = finnEllerOpprettBehandling(connection, sak)

            val request = SamordneVedtakRequest(
                pid = "12345678910",
                vedtakId = "1",
                sakId = sak.id.id,
                virkFom = LocalDate.of(2020, 1, 1),
                virkTom = LocalDate.of(2999, 1, 1),
                fagomrade = "AAP",
                ytelseType = "AAP",
                etterbetaling = false,
                utvidetFrist = null,
            )
            val varslet = SamVarsling(
                vedtakId = 1L,
                varslet = true,
                førstegangsbehandling = true,
                endringIRettighetstype = false,
                antallTpYtelser = 2,
                request = request,
                respons = SamordneVedtakRespons(ventPaaSvar = true),
            )
            val ikkeVarslet = varslet.copy(varslet = false, antallTpYtelser = 0, respons = null)

            repo.lagre(behandling.id, varslet)
            repo.lagre(behandling.id, ikkeVarslet)

            assertThat(repo.hent(behandling.id).map { it.varsling }).containsExactly(varslet, ikkeVarslet)
        }
    }
}
