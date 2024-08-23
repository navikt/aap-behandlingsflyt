package no.nav.aap.behandlingsflyt.faktagrunnlag.barn

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.dbconnect.transaction
import no.nav.aap.behandlingsflyt.dbtest.InitTestDatabase
import no.nav.aap.behandlingsflyt.dbtestdata.ident
import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.faktagrunnlag.arbeidsevne.FakePdlGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.BarnRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.OppgittBarn
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.EndringType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Årsak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Saksnummer
import no.nav.aap.verdityper.Periode
import no.nav.aap.verdityper.sakogbehandling.Ident
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class BarnRepositoryTest {

    @Test
    fun `Finner ikke barn hvis det ikke finnes barn`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = behandling(connection, sak)

            val barnRepository = BarnRepository(connection)
            val barn = barnRepository.hentHvisEksisterer(behandling.id)
            assertThat(barn?.registerbarn?.identer).isNullOrEmpty()
        }
    }

    @Test
    fun `Lagrer og henter barn`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = behandling(connection, sak)

            val barnRepository = BarnRepository(connection)
            val barnListe = setOf(Ident("12345678910"))

            barnRepository.lagreRegisterBarn(behandling.id, barnListe)
            val barn = barnRepository.hent(behandling.id)
            assertThat(barn.registerbarn?.identer).size().isEqualTo(1)
        }
    }
    
    @Test
    fun `Henter barn for saker`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = behandling(connection, sak)
            val barnRepository = BarnRepository(connection)
            val registerBarn = setOf(Ident("1234567890"), Ident("1337"))
            val oppgittBarn = OppgittBarn(null, setOf(Ident("0987654321")))
            barnRepository.lagreRegisterBarn(behandling.id, registerBarn)
            barnRepository.lagreOppgitteBarn(behandling.id, oppgittBarn)
            
            val oppgitteBarnForSak = barnRepository.hentOppgitteBarnForSaker(listOf(sak.saksnummer, Saksnummer("1234")))
            val registerBarnForSak = barnRepository.hentRegisterBarnForSaker(listOf(sak.saksnummer, Saksnummer("1234")))
            assertThat(registerBarnForSak[sak.saksnummer]).hasSize(2)
            assertThat(registerBarnForSak[Saksnummer("1234")]).isNullOrEmpty()
            assertThat(oppgitteBarnForSak[sak.saksnummer]).hasSize(1)
            assertThat(oppgitteBarnForSak[Saksnummer("1234")]).isNullOrEmpty()
        }
    }

    private companion object {
        private val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))
    }

    private fun sak(connection: DBConnection): Sak {
        return PersonOgSakService(connection, FakePdlGateway).finnEllerOpprett(
            ident(),
            periode
        )
    }

    private fun behandling(connection: DBConnection, sak: Sak): Behandling {
        return SakOgBehandlingService(connection).finnEllerOpprettBehandling(
            sak.saksnummer,
            listOf(Årsak(EndringType.MOTTATT_SØKNAD))
        ).behandling
    }

}