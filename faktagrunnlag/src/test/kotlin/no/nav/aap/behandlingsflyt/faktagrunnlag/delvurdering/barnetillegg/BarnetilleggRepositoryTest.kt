package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.barnetillegg

import kotlinx.coroutines.runBlocking
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.dbconnect.transaction
import no.nav.aap.behandlingsflyt.dbtest.InitTestDatabase
import no.nav.aap.behandlingsflyt.dbtestdata.ident
import no.nav.aap.behandlingsflyt.faktagrunnlag.arbeidsevne.FakePdlGateway
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakOgBehandlingService
import no.nav.aap.verdityper.Periode
import no.nav.aap.verdityper.sakogbehandling.Ident
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class BarnetilleggRepositoryTest{
    @Test
    fun `Lagrer og henter fritaksvurdering`(){
        InitTestDatabase.dataSource.transaction { connection ->
            val sak = runBlocking { sak(connection) }
            val behandling = behandling(connection, sak)

            val barnetilleggRepository = BarnetilleggRepository(connection)
            val barnetilleggPeriode = BarnetilleggPeriode(
                Periode(LocalDate.of(2024,1,1), LocalDate.of(2024,1,1)),
                listOf(Ident("12345678910")).toSet()
            )

            barnetilleggRepository.lagre(behandling.id,listOf(barnetilleggPeriode))

            val barnetilleggGrunnlag = barnetilleggRepository.hentHvisEksisterer(
                behandling.id
            )

            assertThat(barnetilleggGrunnlag?.perioder).containsExactly(
                barnetilleggPeriode
            )
        }
    }

    private companion object {
        private val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))
    }

    private suspend fun sak(connection: DBConnection): Sak {
        return PersonOgSakService(connection, FakePdlGateway).finnEllerOpprett(
            ident(),
            periode
        )
    }

    private fun behandling(connection: DBConnection, sak: Sak): Behandling {
        return SakOgBehandlingService(connection).finnEllerOpprettBehandling(sak.saksnummer).behandling
    }
}