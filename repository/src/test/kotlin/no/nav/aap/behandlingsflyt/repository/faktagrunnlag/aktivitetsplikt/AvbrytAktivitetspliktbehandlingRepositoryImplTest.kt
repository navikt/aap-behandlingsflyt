package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.aktivitetsplikt

import no.nav.aap.behandlingsflyt.behandling.avbrytaktivitetspliktbehandling.AvbrytAktivitetspliktbehandlingVurdering
import no.nav.aap.behandlingsflyt.behandling.avbrytaktivitetspliktbehandling.AvbrytAktivitetspliktbehandlingÅrsak
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.help.opprettSak
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.avbrytaktivitetspliktbehandling.AvbrytAktivitetspliktbehandlingRepositoryImpl
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.komponenter.verdityper.Bruker
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.time.LocalDate

class AvbrytAktivitetspliktbehandlingRepositoryImplTest {
    companion object {
        private lateinit var dataSource: TestDataSource

        @BeforeAll
        @JvmStatic
        fun setup() {
            dataSource = TestDataSource()
        }

        @AfterAll
        @JvmStatic
        fun tearDown() = dataSource.close()
    }

    @Test
    fun `Kan lagre og lese ut grunnlag med vurdering`() {
        dataSource.transaction { connection ->
            val sak = opprettSak(connection, LocalDate.now())
            val behandling = finnEllerOpprettBehandling(connection, sak)

            val avbrytAktivitetspliktbehandlingRepository = AvbrytAktivitetspliktbehandlingRepositoryImpl(connection)
            val avbrytAktivitetspliktbehandlingVurdering = AvbrytAktivitetspliktbehandlingVurdering(
                årsak = AvbrytAktivitetspliktbehandlingÅrsak.BEHANDLINGEN_BLE_OPPRETTET_VED_EN_FEIL,
                begrunnelse = "Trykket feil",
                vurdertAv = Bruker("Saksbehandler")
            )

            avbrytAktivitetspliktbehandlingRepository.lagre(behandling.id, avbrytAktivitetspliktbehandlingVurdering)
            val hentetVurdering = avbrytAktivitetspliktbehandlingRepository.hentHvisEksisterer(behandling.id)?.vurdering

            assertThat(hentetVurdering).isNotNull
            assertThat(hentetVurdering?.årsak).isEqualTo(AvbrytAktivitetspliktbehandlingÅrsak.BEHANDLINGEN_BLE_OPPRETTET_VED_EN_FEIL)
        }
    }
}