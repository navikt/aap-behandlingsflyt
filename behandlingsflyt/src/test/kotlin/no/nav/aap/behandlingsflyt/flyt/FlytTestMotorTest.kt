package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.behandlingsflyt.prosessering.ProsesserBehandlingJobbUtfører
import no.nav.aap.behandlingsflyt.test.AlleAvskruddUnleash
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.LocalDateTime

class FlytTestMotorTest : AbstraktFlytOrkestratorTest(AlleAvskruddUnleash::class) {

    @Test
    fun `kjører jobb som ligger rett frem i tid, slik den gjør når klokka er stilt tilbake`() {
        val jobbId = planleggProsesserBehandling(om = Duration.ofMillis(30))

        motor.kjørJobber()

        assertThat(status(jobbId)).isEqualTo("FERDIG")
    }

    @Test
    fun `kjører ikke jobb som er utsatt med vilje`() {
        val jobbId = planleggProsesserBehandling(om = Duration.ofHours(1))

        motor.kjørJobber()

        assertThat(status(jobbId)).isEqualTo("KLAR")
    }

    private fun planleggProsesserBehandling(om: Duration): Long {
        val (sak, behandling) = sendInnFørsteSøknad()
        return dataSource.transaction { connection ->
            JobbRepository(connection).leggTil(
                JobbInput(ProsesserBehandlingJobbUtfører)
                    .forBehandling(sak.id.toLong(), behandling.id.toLong())
                    .medNesteKjøring(LocalDateTime.now().plus(om))
            )
        }
    }

    private fun status(jobbId: Long): String = dataSource.transaction(readOnly = true) { connection ->
        connection.queryFirst("SELECT status FROM jobb WHERE id = ?") {
            setParams { setLong(1, jobbId) }
            setRowMapper { it.getString("status") }
        }
    }
}
