package no.nav.aap.behandlingsflyt.faktagrunnlag.beregning

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.dbconnect.transaction
import no.nav.aap.behandlingsflyt.dbtest.InitTestDatabase
import no.nav.aap.behandlingsflyt.dbtestdata.ident
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.sakRepository
import no.nav.aap.verdityper.GUnit
import no.nav.aap.verdityper.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class BeregningsgrunnlagRepositoryTest {

    @Test
    fun `Lagre og hente opp beregningsgrunnlaget med uføre og yrkesskade`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = behandling(connection, sak)

            val grunnlag11_19Standard = Grunnlag11_19(GUnit(1))
            val grunnlagYrkesskadeStandard = GrunnlagYrkesskade(GUnit(2), grunnlag11_19Standard)
            val grunnlag11_19Ytterligere = Grunnlag11_19(GUnit(3))
            val grunnlagYrkesskadeYtterligere = GrunnlagYrkesskade(GUnit(4), grunnlag11_19Ytterligere)
            val grunnlagUføre = GrunnlagUføre(
                GUnit(4),
                GrunnlagUføre.Type.YTTERLIGERE_NEDSATT,
                grunnlagYrkesskadeStandard,
                grunnlagYrkesskadeYtterligere
            )

            val beregningsgrunnlagRepository = BeregningsgrunnlagRepository(connection)

            beregningsgrunnlagRepository.lagre(behandling.id, grunnlagUføre)

            val beregningsgrunnlag = beregningsgrunnlagRepository.hentHvisEksisterer(behandling.id)

            assertThat(beregningsgrunnlag).isEqualTo(grunnlagUføre)
        }
    }

    @Test
    fun `Lagre og hente opp beregningsgrunnlaget med uføre uten yrkesskade`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = behandling(connection, sak)

            val grunnlag11_19Standard = Grunnlag11_19(GUnit(1))
            val grunnlag11_19Ytterligere = Grunnlag11_19(GUnit(3))
            val grunnlagUføre =
                GrunnlagUføre(GUnit(4), GrunnlagUføre.Type.STANDARD, grunnlag11_19Standard, grunnlag11_19Ytterligere)

            val beregningsgrunnlagRepository = BeregningsgrunnlagRepository(connection)

            beregningsgrunnlagRepository.lagre(behandling.id, grunnlagUføre)

            val beregningsgrunnlag = beregningsgrunnlagRepository.hentHvisEksisterer(behandling.id)

            assertThat(beregningsgrunnlag).isEqualTo(grunnlagUføre)
        }
    }

    @Test
    fun `Lagre og hente opp beregningsgrunnlaget uten uføre og yrkesskade`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = behandling(connection, sak)

            val grunnlag11_19Standard = Grunnlag11_19(GUnit(1))

            val beregningsgrunnlagRepository = BeregningsgrunnlagRepository(connection)

            beregningsgrunnlagRepository.lagre(behandling.id, grunnlag11_19Standard)

            val beregningsgrunnlag = beregningsgrunnlagRepository.hentHvisEksisterer(behandling.id)

            assertThat(beregningsgrunnlag).isEqualTo(grunnlag11_19Standard)
        }
    }

    private companion object {
        private val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))
    }

    private fun sak(connection: DBConnection): Sak {
        return sakRepository(connection).finnEllerOpprett(
            person = PersonRepository(connection).finnEllerOpprett(ident()),
            periode = periode
        )
    }

    private fun behandling(connection: DBConnection, sak: Sak): Behandling {
        return SakOgBehandlingService(connection).finnEllerOpprettBehandling(sak.saksnummer).behandling
    }
}
