package no.nav.aap.behandlingsflyt.hendelse.oppgavestyring

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Saksnummer
import no.nav.aap.behandlingsflyt.test.Fakes
import no.nav.aap.behandlingsflyt.kontrakt.sak.Status
import no.nav.aap.verdityper.sakogbehandling.TypeBehandling
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

@Disabled
class OppgavestyringGatewayTest {
    companion object {
        private val fakes = Fakes()

        @JvmStatic
        @AfterAll
        fun afterAll() {
            fakes.close()
        }
    }

    @Disabled
    @Test
    fun `varsleHendelse returnerer 200 når alt er fint`() {
        // USikker på hva denne testen bidrar med når den implisitt er testet gjennom flyttest
        val gateway = OppgavestyringGateway
        assertDoesNotThrow {
            gateway.varsleHendelse(
                BehandlingsFlytStoppetHendelseDTO(
                    personident = "124512451245",
                    Saksnummer("24352363"),
                    opprettetTidspunkt = LocalDateTime.now(),
                    status = Status.OPPRETTET,
                    behandlingType = TypeBehandling.Klage,
                    avklaringsbehov = emptyList(),
                    referanse = BehandlingReferanse()
                )
            )
        }

    }
}