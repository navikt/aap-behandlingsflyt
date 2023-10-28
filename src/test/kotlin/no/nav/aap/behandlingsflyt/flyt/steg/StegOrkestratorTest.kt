package no.nav.aap.behandlingsflyt.flyt.steg

import no.nav.aap.behandlingsflyt.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.dbstuff.DBConnection
import no.nav.aap.behandlingsflyt.dbstuff.MockConnection
import no.nav.aap.behandlingsflyt.flyt.FlytKontekst
import no.nav.aap.behandlingsflyt.flyt.behandlingstyper.Førstegangsbehandling
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class StegOrkestratorTest {

    private val transaksjonsconnection = DBConnection(MockConnection())

    @Test
    fun `ved avklaringsbehov skal vi gå gjennom statusene START-UTFØRER-AVKARLINGSPUNKT`() {
        val behandling = BehandlingRepository.opprettBehandling(1L, emptyList())
        assertThat(behandling.type).isEqualTo(Førstegangsbehandling)

        val kontekst = FlytKontekst(1L, behandling.id)

        val resultat = StegOrkestrator(transaksjonsconnection, TestFlytSteg).utfør(kontekst, behandling)

        assertThat(resultat).isNotNull

        assertThat(behandling.stegHistorikk()).hasSize(3)
        assertThat(behandling.stegHistorikk()[0].tilstand.status()).isEqualTo(StegStatus.START)
        assertThat(behandling.stegHistorikk()[1].tilstand.status()).isEqualTo(StegStatus.UTFØRER)
        assertThat(behandling.stegHistorikk()[2].tilstand.status()).isEqualTo(StegStatus.AVKLARINGSPUNKT)
    }
}
