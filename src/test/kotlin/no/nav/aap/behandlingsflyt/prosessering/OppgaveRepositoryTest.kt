package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.Periode
import no.nav.aap.behandlingsflyt.dbconnect.InitTestDatabase
import no.nav.aap.behandlingsflyt.dbconnect.transaction
import no.nav.aap.behandlingsflyt.prosessering.retry.OPPGAVE_TYPE
import no.nav.aap.behandlingsflyt.prosessering.retry.RetryService
import no.nav.aap.behandlingsflyt.sak.Ident
import no.nav.aap.behandlingsflyt.sak.PersonRepository
import no.nav.aap.behandlingsflyt.sak.Sak
import no.nav.aap.behandlingsflyt.sak.sakRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.Ignore
import org.junit.jupiter.api.Test
import java.time.LocalDate


class OppgaveRepositoryTest {
    companion object {
        val dataSource = InitTestDatabase.dataSource
    }

    @Ignore
    @Test
    fun `skal plukke første opprettede oppgave`() {
        lateinit var sak: Sak
        lateinit var sak2: Sak
        dataSource.transaction { connection ->
            val personRepository = PersonRepository(connection)
            val sakRepository = sakRepository(connection)
            val person = personRepository.finnEllerOpprett(Ident("12312312312312313"))
            val person1 = personRepository.finnEllerOpprett(Ident("12312312356756756"))
            sak = sakRepository.finnEllerOpprett(person, Periode(LocalDate.now().minusYears(3), LocalDate.now()))
            sak2 = sakRepository.finnEllerOpprett(person1, Periode(LocalDate.now().minusYears(3), LocalDate.now()))
        }

        var plukketOppgave: OppgaveInput? = null
        dataSource.transaction {
            val repository = OppgaveRepository(it)

            repository.leggTil(OppgaveInput(ProsesserBehandlingOppgave).forBehandling(sakId = sak.id, null))
            repository.leggTil(OppgaveInput(ProsesserBehandlingOppgave).forBehandling(sakId = sak2.id, null))
            plukketOppgave = repository.plukkOppgave()

            assertThat(plukketOppgave).isNotNull
            assertThat(plukketOppgave!!.id).isEqualTo(1)
            assertThat(plukketOppgave!!.sakId()).isEqualTo(sak.id)
            repository.markerKjørt(plukketOppgave!!)
        }

        dataSource.transaction {
            val repository = OppgaveRepository(it)
            repository.leggTil(OppgaveInput(ProsesserBehandlingOppgave).forBehandling(sak2.id, null))
            repository.leggTil(OppgaveInput(ProsesserBehandlingOppgave).forBehandling(sak2.id, null))

            plukketOppgave = repository.plukkOppgave()
            assertThat(plukketOppgave!!.id).isEqualTo(2)
            assertThat(plukketOppgave!!.sakId()).isEqualTo(sak2.id)

            repository.markerFeilet(plukketOppgave!!, IllegalStateException("feilet"))
        }
        dataSource.transaction {
            val repository = OppgaveRepository(it)
            plukketOppgave = repository.plukkOppgave()

            assertThat(plukketOppgave).isNull()
        }
        dataSource.transaction {
            RetryService(it).enable()
            val repository = OppgaveRepository(it)
            plukketOppgave = repository.plukkOppgave()

            assertThat(plukketOppgave).isNotNull()
            assertThat(plukketOppgave!!.type()).isEqualTo(OPPGAVE_TYPE)
        }
    }
}
