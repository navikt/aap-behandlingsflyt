package no.nav.aap.behandlingsflyt.flyt.steg

import no.nav.aap.behandlingsflyt.verdityper.Periode
import no.nav.aap.behandlingsflyt.behandling.behandlingRepository
import no.nav.aap.behandlingsflyt.dbconnect.InitTestDatabase
import no.nav.aap.behandlingsflyt.dbconnect.transaction
import no.nav.aap.behandlingsflyt.flyt.behandlingstyper.Førstegangsbehandling
import no.nav.aap.behandlingsflyt.flyt.tilKontekst
import no.nav.aap.behandlingsflyt.sak.Ident
import no.nav.aap.behandlingsflyt.sak.PersonRepository
import no.nav.aap.behandlingsflyt.sak.sakRepository
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
            val person = PersonRepository(connection).finnEllerOpprett(ident)
            val sak = sakRepository(connection).finnEllerOpprett(person, periode)
            val behandling = behandlingRepository(connection).opprettBehandling(sak.id, listOf())
            assertThat(behandling.type).isEqualTo(Førstegangsbehandling)

            val kontekst = tilKontekst(behandling)

            val resultat = StegOrkestrator(connection, TestFlytSteg).utfør(kontekst, behandling)

            assertThat(resultat).isNotNull

            assertThat(behandling.stegHistorikk()).hasSize(3)
            assertThat(behandling.stegHistorikk()[0].status()).isEqualTo(StegStatus.START)
            assertThat(behandling.stegHistorikk()[1].status()).isEqualTo(StegStatus.UTFØRER)
            assertThat(behandling.stegHistorikk()[2].status()).isEqualTo(StegStatus.AVKLARINGSPUNKT)
        }
    }
}
