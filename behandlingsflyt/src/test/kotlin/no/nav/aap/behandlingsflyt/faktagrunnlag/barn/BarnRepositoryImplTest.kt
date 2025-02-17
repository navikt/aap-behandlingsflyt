package no.nav.aap.behandlingsflyt.faktagrunnlag.barn

import no.nav.aap.behandlingsflyt.faktagrunnlag.FakePdlGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.GrunnlagKopierer
import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.BarnRepositoryImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.OppgitteBarn
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.PersonRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Årsak
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.ÅrsakTilBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.test.ident
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class BarnRepositoryImplTest {

    @Test
    fun `Finner ikke barn hvis det ikke finnes barn`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = behandling(connection, sak)

            val barnRepository = BarnRepositoryImpl(connection)
            val barn = barnRepository.hentHvisEksisterer(behandling.id)
            assertThat(barn?.registerbarn?.identer).isNullOrEmpty()
        }
    }

    @Test
    fun `Lagrer og henter barn`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = behandling(connection, sak)

            val barnRepository = BarnRepositoryImpl(connection)
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
            val barnRepository = BarnRepositoryImpl(connection)
            val registerBarn = setOf(Ident("1234567890"), Ident("1337"))
            val oppgitteBarn = OppgitteBarn(null, setOf(Ident("0987654321")))
            barnRepository.lagreRegisterBarn(behandling.id, registerBarn)
            barnRepository.lagreOppgitteBarn(behandling.id, oppgitteBarn)

            val oppgitteBarnForSak = barnRepository.hentOppgitteBarnForSaker(listOf(sak.saksnummer, Saksnummer("1234")))
            val registerBarnForSak = barnRepository.hentRegisterBarnForSaker(listOf(sak.saksnummer, Saksnummer("1234")))
            assertThat(registerBarnForSak[sak.saksnummer]).hasSize(2)
            assertThat(registerBarnForSak[Saksnummer("1234")]).isNullOrEmpty()
            assertThat(oppgitteBarnForSak[sak.saksnummer]).hasSize(1)
            assertThat(oppgitteBarnForSak[Saksnummer("1234")]).isNullOrEmpty()
        }
    }

    @Test
    fun `Kopiering av barn fra en behandling til en annen`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val barnRepository = BarnRepositoryImpl(connection)
            // Given
            val sak = sak(connection)
            val gammelBehandling = behandling(connection, sak)
            barnRepository.lagreOppgitteBarn(gammelBehandling.id, OppgitteBarn(identer = setOf(Ident("1"))))

            // When
            BehandlingRepositoryImpl(connection).oppdaterBehandlingStatus(gammelBehandling.id, Status.AVSLUTTET)
            val nyBehandling = behandling(connection, sak)
            barnRepository.kopier(gammelBehandling.id, nyBehandling.id)

            // Then
            val gamleOppgitteBarn = barnRepository.hent(nyBehandling.id).oppgitteBarn?.identer
            val nyeOppgitteBarn = barnRepository.hent(nyBehandling.id).oppgitteBarn?.identer
            assertThat(nyeOppgitteBarn).isEqualTo(gamleOppgitteBarn)
        }
    }

    private companion object {
        private val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))
    }

    private fun sak(connection: DBConnection): Sak {
        return PersonOgSakService(
            FakePdlGateway,
            PersonRepositoryImpl(connection),
            SakRepositoryImpl(connection)
        ).finnEllerOpprett(
            ident(),
            periode
        )
    }

    private fun behandling(connection: DBConnection, sak: Sak): Behandling {
        return SakOgBehandlingService(
            GrunnlagKopierer(connection), SakRepositoryImpl(connection),
            BehandlingRepositoryImpl(connection)
        ).finnEllerOpprettBehandling(
            sak.saksnummer,
            listOf(Årsak(ÅrsakTilBehandling.MOTTATT_SØKNAD))
        ).behandling
    }
}