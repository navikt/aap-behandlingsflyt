package no.nav.aap.behandlingsflyt.hendelse.oppgavestyring

import no.nav.aap.behandlingsflyt.hendelse.avløp.BehandlingFlytStoppetHendelse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Saksnummer
import no.nav.aap.behandlingsflyt.test.Fakes
import no.nav.aap.verdityper.sakogbehandling.Status
import no.nav.aap.verdityper.sakogbehandling.TypeBehandling
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.test.Ignore

class OppgavestyringGatewayTest {
    companion object {
        private val fakes = Fakes()

        @JvmStatic
        @AfterAll
        fun afterAll() {
            fakes.close()
        }
    }

     //TODO: Fix this test
    @Test
    fun `varsleHendelse returnerer 200 når alt er fint`() {
        val gateway = OppgavestyringGateway
        assertDoesNotThrow {
            gateway.varsleHendelse(
                BehandlingFlytStoppetHendelse(
                    personident = "124512451245",
                    Saksnummer("24352363"),
                    opprettetTidspunkt = LocalDateTime.now(),
                    status = Status.PÅ_VENT,
                    behandlingType = TypeBehandling.Klage,
                    avklaringsbehov = emptyList(),
                    referanse = BehandlingReferanse("yolo")
                )
            )
        }

    }
}