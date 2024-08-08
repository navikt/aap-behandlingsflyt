package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.FakePdlGateway
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.dbconnect.transaction
import no.nav.aap.behandlingsflyt.dbtest.InitTestDatabase
import no.nav.aap.behandlingsflyt.dbtestdata.ident
import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.flate.HelseinstitusjonVurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.EndringType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Årsak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.verdityper.Periode
import no.nav.aap.verdityper.dokument.JournalpostId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

class HelseinstitusjonRepositoryTest {

    @Test
    fun `Forventer at en soningsvurdering kan lagres`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = behandling(connection, sak)

            val helseinstitusjonRepository = HelseinstitusjonRepository(connection)

            val helseinstitusjonVurdering = HelseinstitusjonVurdering(
                dokumenterBruktIVurdering = listOf(JournalpostId("vsafdvasfv")),
                begrunnelse = "Hello there",
                faarFriKostOgLosji = false
            )

            helseinstitusjonRepository.lagre(behandling.id, helseinstitusjonVurdering)
        }
    }

    @Test
    fun `Når to soningsvurderinger blir lagret på samme behandling forventer jeg å få den siste vurderingen`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = behandling(connection, sak)

            val helseinstitusjonRepository = HelseinstitusjonRepository(connection)

            val helseinstitusjonVurdering1 = HelseinstitusjonVurdering(
                dokumenterBruktIVurdering = emptyList(),
                begrunnelse = "sdfgsdg",
                faarFriKostOgLosji = false
            )

            val helseinstitusjonVurdering2 = HelseinstitusjonVurdering(
                dokumenterBruktIVurdering = emptyList(),
                begrunnelse = "sdfgsdg",
                faarFriKostOgLosji = true
            )

            helseinstitusjonRepository.lagre(behandling.id, helseinstitusjonVurdering1)
            helseinstitusjonRepository.lagre(behandling.id, helseinstitusjonVurdering2)

            val faktisk = helseinstitusjonRepository.hentAktivHelseinstitusjonVurdering(behandling.id)

            assertThat(faktisk).isEqualTo(helseinstitusjonVurdering2)
        }
    }

    @Test
    fun `Forvenet å hente ut lik soningsvurdering som ble skrevet til database`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = behandling(connection, sak)

            val helseinstitusjonRepository = HelseinstitusjonRepository(connection)

            val helseinstitusjonVurdering = HelseinstitusjonVurdering(
                dokumenterBruktIVurdering = listOf(JournalpostId("yolo"), JournalpostId("swag")),
                begrunnelse = "sdfgsdg",
                faarFriKostOgLosji = true
            )

            helseinstitusjonRepository.lagre(behandling.id, helseinstitusjonVurdering)

            val lagretSoningsvurdering = helseinstitusjonRepository.hentAktivHelseinstitusjonVurdering(behandling.id)

            assertThat(helseinstitusjonVurdering).isEqualTo(lagretSoningsvurdering)
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
            listOf(Årsak(EndringType.MOTTATT_SØKNAD))
        ).behandling
    }
}