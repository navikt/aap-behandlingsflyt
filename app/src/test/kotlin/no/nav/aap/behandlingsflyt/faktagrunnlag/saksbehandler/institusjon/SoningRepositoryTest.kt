package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.FakePdlGateway
import no.nav.aap.behandlingsflyt.dbtestdata.ident
import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Årsak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.verdityper.dokument.JournalpostId
import no.nav.aap.verdityper.flyt.ÅrsakTilBehandling
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

class SoningRepositoryTest {

    @Test
    fun `Forventer at en soningsvurdering kan lagres`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = behandling(connection, sak)

            val soningRepository = SoningRepository(connection)

            val soningsvurdering = Soningsvurdering(
                dokumenterBruktIVurdering = listOf(JournalpostId("vsafdvasfv")),
                soningUtenforFengsel = true,
                førsteArbeidsdag = LocalDate.now(),
                arbeidUtenforAnstalt = true,
                begrunnelse = "YOLO",
                periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))
            )

            soningRepository.lagre(behandling.id, soningsvurdering)
        }
    }

    @Test
    fun `Når to soningsvurderinger blir lagret på samme behandling forventer jeg å få den siste vurderingen`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = behandling(connection, sak)

            val soningRepository = SoningRepository(connection)

            val soningsvurdering1 = Soningsvurdering(
                dokumenterBruktIVurdering = emptyList(),
                soningUtenforFengsel = true,
                periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))
            )

            val soningsvurdering2 = Soningsvurdering(
                dokumenterBruktIVurdering = emptyList(),
                soningUtenforFengsel = false,
                periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))
            )

            soningRepository.lagre(behandling.id, soningsvurdering1)
            soningRepository.lagre(behandling.id, soningsvurdering2)

            val faktisk = soningRepository.hentAktiveSoningsvurdering(behandling.id)

            assertThat(faktisk).isEqualTo(soningsvurdering2)
        }
    }

    @Test
    fun `Forvenet å hente ut lik soningsvurdering som ble skrevet til database`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = behandling(connection, sak)

            val soningRepository = SoningRepository(connection)

            val soningsvurdering = Soningsvurdering(
                dokumenterBruktIVurdering = listOf(JournalpostId("yolo"), JournalpostId("swag")),
                soningUtenforFengsel = true,
                førsteArbeidsdag = LocalDate.now(),
                periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))
            )

            soningRepository.lagre(behandling.id, soningsvurdering)

            val lagretSoningsvurdering = soningRepository.hentAktiveSoningsvurdering(behandling.id)

            assertThat(soningsvurdering).isEqualTo(lagretSoningsvurdering)
        }
    }

    @Test
    fun `Forvener exception når vi prøver å hente soningsvurdering som ikke eksisterer`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = behandling(connection, sak)

            val soningRepository = SoningRepository(connection)

            assertThrows<NoSuchElementException> {
                soningRepository.hentAktiveSoningsvurdering(behandling.id)
            }
        }
    }

    private companion object {
        private val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))
    }

    private fun sak(connection: DBConnection): Sak {
        return PersonOgSakService(connection, FakePdlGateway).finnEllerOpprett(ident(), periode)
    }

    private fun behandling(connection: DBConnection, sak: Sak): Behandling {
        return SakOgBehandlingService(connection).finnEllerOpprettBehandling(
            sak.saksnummer,
            listOf(Årsak(ÅrsakTilBehandling.MOTTATT_SØKNAD))
        ).behandling
    }
}