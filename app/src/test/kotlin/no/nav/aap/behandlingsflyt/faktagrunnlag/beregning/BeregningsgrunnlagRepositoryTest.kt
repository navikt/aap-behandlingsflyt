package no.nav.aap.behandlingsflyt.faktagrunnlag.beregning

import no.nav.aap.behandlingsflyt.verdityper.Periode
import no.nav.aap.behandlingsflyt.behandling.Behandling
import no.nav.aap.behandlingsflyt.behandling.behandlingRepository
import no.nav.aap.behandlingsflyt.beregning.Grunnlag11_19
import no.nav.aap.behandlingsflyt.beregning.GrunnlagUføre
import no.nav.aap.behandlingsflyt.beregning.GrunnlagYrkesskade
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.dbconnect.InitTestDatabase
import no.nav.aap.behandlingsflyt.dbconnect.transaction
import no.nav.aap.behandlingsflyt.faktagrunnlag.inntekt.GUnit
import no.nav.aap.behandlingsflyt.ident
import no.nav.aap.behandlingsflyt.sak.PersonRepository
import no.nav.aap.behandlingsflyt.sak.Sak
import no.nav.aap.behandlingsflyt.sak.sakRepository
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
        val behandling = behandlingRepository(connection).finnSisteBehandlingFor(sak.id)
        if (behandling == null || behandling.status().erAvsluttet()) {
            return behandlingRepository(connection).opprettBehandling(sak.id, listOf())
        }
        return behandling
    }
}
