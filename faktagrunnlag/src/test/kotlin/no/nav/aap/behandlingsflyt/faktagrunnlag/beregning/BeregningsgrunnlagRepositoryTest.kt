package no.nav.aap.behandlingsflyt.faktagrunnlag.beregning

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.dbconnect.transaction
import no.nav.aap.behandlingsflyt.dbtest.InitTestDatabase
import no.nav.aap.behandlingsflyt.dbtestdata.ident
import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.faktagrunnlag.arbeidsevne.FakePdlGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.BeregningsgrunnlagRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.Grunnlag11_19
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.GrunnlagUføre
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.GrunnlagYrkesskade
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.EndringType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Årsak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.verdityper.Beløp
import no.nav.aap.verdityper.GUnit
import no.nav.aap.verdityper.Periode
import no.nav.aap.verdityper.Prosent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.Year

class BeregningsgrunnlagRepositoryTest {

    @Test
    fun `Lagre og hente opp beregningsgrunnlaget med uføre og yrkesskade`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = behandling(connection, sak)

            val grunnlag11_19Standard =
                Grunnlag11_19(GUnit(1),false,false)
            val grunnlag11_19Ytterligere =
                Grunnlag11_19(GUnit(3),false,false)
            val grunnlagUføre = GrunnlagUføre(
                GUnit(4),
                GrunnlagUføre.Type.YTTERLIGERE_NEDSATT,
                grunnlag11_19Standard,
                grunnlag11_19Ytterligere,
                Prosent(50),
                emptyList(),
                Beløp(0),
                Year.of(2022),
                false,
                false,
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

            val grunnlag11_19Standard =
                Grunnlag11_19(GUnit(1),false,false)
            val grunnlag11_19Ytterligere =
                Grunnlag11_19(GUnit(3),false,false)
            val grunnlagUføre =
                GrunnlagUføre(
                    GUnit(4),
                    GrunnlagUføre.Type.STANDARD,
                    grunnlag11_19Standard,
                    grunnlag11_19Ytterligere,
                    Prosent(50),
                    emptyList(),
                    Beløp(0),
                    Year.of(2022),
                    false,
                    false,
                )

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

            val grunnlag11_19Standard =
                Grunnlag11_19(GUnit(1),false,false)

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
        return PersonOgSakService(connection, FakePdlGateway).finnEllerOpprett(ident(), periode)
    }

    private fun behandling(connection: DBConnection, sak: Sak): Behandling {
        return SakOgBehandlingService(connection).finnEllerOpprettBehandling(
            sak.saksnummer,
            listOf(Årsak(EndringType.MOTTATT_SØKNAD))
        ).behandling
    }
}
