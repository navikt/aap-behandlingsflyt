package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.dbstuff.InitTestDatabase
import no.nav.aap.behandlingsflyt.dbstuff.transaction
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class OppgaveRepositoryTest {
    companion object {
        val dataSource = InitTestDatabase.dataSource
    }

    @Test
    fun `skal plukke første opprettede oppgave`() {
        var plukketOppgave: OppgaveInput? = null
        dataSource.transaction { connection ->
            val repository = OppgaveRepository(connection)
            repository.leggTil(OppgaveInput(ProsesserBehandlingOppgave).forBehandling(null, null))
            repository.leggTil(OppgaveInput(ProsesserBehandlingOppgave).forBehandling(null, null))
            repository.leggTil(OppgaveInput(ProsesserBehandlingOppgave).forBehandling(null, null))
            repository.leggTil(OppgaveInput(ProsesserBehandlingOppgave).forBehandling(null, null))
            repository.leggTil(OppgaveInput(ProsesserBehandlingOppgave).forBehandling(null, null))
            repository.leggTil(OppgaveInput(ProsesserBehandlingOppgave).forBehandling(null, null))

            plukketOppgave = repository.plukkOppgave()
        }

        assertThat(plukketOppgave).isNotNull
        assertThat(plukketOppgave?.id).isEqualTo(1)

        dataSource.transaction {
            it.execute("TRUNCATE TABLE OPPGAVE cascade")
        }
    }
}
