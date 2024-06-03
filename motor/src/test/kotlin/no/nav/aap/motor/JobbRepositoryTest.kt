package no.nav.aap.motor

import no.nav.aap.behandlingsflyt.dbconnect.transaction
import no.nav.aap.behandlingsflyt.dbtest.InitTestDatabase
import no.nav.aap.motor.help.TestBehandlingRepository
import no.nav.aap.motor.help.TestPersonRepository
import no.nav.aap.motor.help.TestSakRepository
import no.nav.aap.motor.help.TullTestJobbUtfører
import no.nav.aap.motor.help.TøysOgTullTestJobbUtfører
import no.nav.aap.motor.help.TøysTestJobbUtfører
import no.nav.aap.verdityper.Periode
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import no.nav.aap.verdityper.sakogbehandling.Ident
import no.nav.aap.verdityper.sakogbehandling.SakId
import no.nav.aap.verdityper.sakogbehandling.TypeBehandling
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class JobbRepositoryTest {

    private val dataSource = InitTestDatabase.dataSource

    init {
        JobbType.leggTil(TøysOgTullTestJobbUtfører)
        JobbType.leggTil(TøysTestJobbUtfører)
        JobbType.leggTil(TullTestJobbUtfører)
    }

    @Test
    fun `skal plukke jobber på sak i en bestemt rekkefølge`() {
        val plukketIRekkefølge = LinkedList<JobbInput>()

        val (sakId, behandlingId) = opprettTestSakOgBehandling()

        val last = LocalDateTime.now().minusMinutes(1)
        val second = LocalDateTime.now().minusHours(1)
        val first = LocalDateTime.now().minusDays(1)

        dataSource.transaction { connection ->
            val jobbRepository = JobbRepository(connection)
            jobbRepository.leggTil(
                JobbInput(TøysTestJobbUtfører).forBehandling(sakId, behandlingId).medNesteKjøring(
                    last
                )
            )
            jobbRepository.leggTil(
                JobbInput(TullTestJobbUtfører).forBehandling(sakId, behandlingId).medNesteKjøring(
                    second
                )
            )
            jobbRepository.leggTil(
                JobbInput(TøysOgTullTestJobbUtfører).forBehandling(sakId, behandlingId).medNesteKjøring(
                    first
                )
            )
        }

        dataSource.transaction { connection ->
            val jobbRepository = JobbRepository(connection)
            var plukket = jobbRepository.plukkJobb()
            while (plukket != null) {
                plukketIRekkefølge.add(plukket)
                jobbRepository.markerKjørt(plukket)
                plukket = jobbRepository.plukkJobb()
            }
        }

        assertThat(plukketIRekkefølge).hasSize(3)
        assertThat(plukketIRekkefølge[0].jobb.type()).isEqualTo(TøysOgTullTestJobbUtfører.type())
        assertThat(plukketIRekkefølge[1].jobb.type()).isEqualTo(TullTestJobbUtfører.type())
        assertThat(plukketIRekkefølge[2].jobb.type()).isEqualTo(TøysTestJobbUtfører.type())
    }

    @Test
    fun `skal bare plukke frem til en jobb feiler`() {
        val plukketIRekkefølge = LinkedList<JobbInput>()

        val (sakId, behandlingId) = opprettTestSakOgBehandling()

        val last = LocalDateTime.now().minusMinutes(1)
        val second = LocalDateTime.now().minusHours(1)
        val first = LocalDateTime.now().minusDays(1)

        dataSource.transaction { connection ->
            val jobbRepository = JobbRepository(connection)
            jobbRepository.leggTil(
                JobbInput(TøysTestJobbUtfører).forBehandling(sakId, behandlingId).medNesteKjøring(
                    last
                )
            )
            jobbRepository.leggTil(
                JobbInput(TullTestJobbUtfører).forBehandling(sakId, behandlingId).medNesteKjøring(
                    second
                )
            )
            jobbRepository.leggTil(
                JobbInput(TøysOgTullTestJobbUtfører).forBehandling(sakId, behandlingId).medNesteKjøring(
                    first
                )
            )
        }

        dataSource.transaction { connection ->
            val jobbRepository = JobbRepository(connection)
            var plukket = jobbRepository.plukkJobb()
            while (plukket != null) {
                plukketIRekkefølge.add(plukket)
                if (plukket.jobb.type() == TullTestJobbUtfører.type()) {
                    jobbRepository.markerFeilet(plukket, IllegalStateException())
                } else {
                    jobbRepository.markerKjørt(plukket)
                }
                plukket = jobbRepository.plukkJobb()
            }
        }

        assertThat(plukketIRekkefølge).hasSize(4)
        assertThat(plukketIRekkefølge[0].jobb.type()).isEqualTo(TøysOgTullTestJobbUtfører.type())
        assertThat(plukketIRekkefølge[1].jobb.type()).isEqualTo(TullTestJobbUtfører.type())
        assertThat(plukketIRekkefølge[2].jobb.type()).isEqualTo(TullTestJobbUtfører.type())
        assertThat(plukketIRekkefølge[3].jobb.type()).isEqualTo(TullTestJobbUtfører.type())
    }

    private fun opprettTestSakOgBehandling(): Pair<SakId, BehandlingId> {
        return dataSource.transaction { connection ->
            val person =
                TestPersonRepository(connection).finnEllerOpprett(listOf(Ident(System.currentTimeMillis().toString())))
            val sakId = TestSakRepository(connection).opprett(person, Periode(LocalDate.now(), LocalDate.now()))
            val behandlingId = TestBehandlingRepository(connection).opprettBehandling(
                sakId,
                TypeBehandling.Førstegangsbehandling
            )
            Pair(sakId, behandlingId)
        }

    }
}