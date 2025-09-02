package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.aktivitetsplikt

import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.help.opprettSak
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDatabase
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import javax.sql.DataSource

class Prosessert11_7VurderingRepositoryImplTest {
    @TestDatabase
    lateinit var dataSource: DataSource

    private val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))


    @Test
    fun `Kan skrive og lese`() {
        dataSource.transaction { connection ->
            val prosessert11_7VurderingRepository = Prosessert11_7VurderingRepositoryRepositoryImpl(connection)

            val sak = opprettSak(connection, periode)
            val behandling = finnEllerOpprettBehandling(connection, sak)
            val aktivitetspliktBehandling = finnEllerOpprettBehandling(connection, sak, Vurderingsbehov.AKTIVITETSPLIKT_11_7)
            prosessert11_7VurderingRepository.lagre(behandling.id, aktivitetspliktBehandling.id)
            
            val prosesserteAktivitetspliktBehandlinger = prosessert11_7VurderingRepository.nyesteProsesserteAktivitetspliktBehandling(behandling.id)
            assertThat(prosesserteAktivitetspliktBehandlinger).containsExactlyInAnyOrder(aktivitetspliktBehandling.id)
        }
    } 
}