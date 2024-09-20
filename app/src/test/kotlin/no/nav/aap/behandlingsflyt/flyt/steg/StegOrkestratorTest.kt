package no.nav.aap.behandlingsflyt.flyt.steg

import kotlinx.coroutines.runBlocking
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.FakePdlGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Årsak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.verdityper.flyt.EndringType
import no.nav.aap.verdityper.flyt.StegStatus
import no.nav.aap.verdityper.sakogbehandling.Ident
import no.nav.aap.verdityper.sakogbehandling.TypeBehandling
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class StegOrkestratorTest {

    companion object {
        val dataSource = InitTestDatabase.dataSource
    }

    @Test
    fun `ved avklaringsbehov skal vi gå gjennom statusene START-UTFØRER-AVKARLINGSPUNKT`() {
        dataSource.transaction { connection ->
            val ident = Ident("123123123126")
            val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))

            val sak = runBlocking {PersonOgSakService(connection, FakePdlGateway).finnEllerOpprett(ident, periode) }
            val behandling = SakOgBehandlingService(connection).finnEllerOpprettBehandling(
                sak.saksnummer,
                listOf(Årsak(EndringType.MOTTATT_SØKNAD))
            ).behandling
            assertThat(behandling.typeBehandling()).isEqualTo(TypeBehandling.Førstegangsbehandling)

            val kontekst = behandling.flytKontekst()

            val resultat = StegOrkestrator(connection, TestFlytSteg).utfør(
                kontekst,
                behandling,
                listOf()
            )

            assertThat(resultat).isNotNull

            assertThat(behandling.stegHistorikk()).hasSize(3)
            assertThat(behandling.stegHistorikk()[0].status()).isEqualTo(StegStatus.START)
            assertThat(behandling.stegHistorikk()[1].status()).isEqualTo(StegStatus.UTFØRER)
            assertThat(behandling.stegHistorikk()[2].status()).isEqualTo(StegStatus.AVKLARINGSPUNKT)
        }
    }
}
