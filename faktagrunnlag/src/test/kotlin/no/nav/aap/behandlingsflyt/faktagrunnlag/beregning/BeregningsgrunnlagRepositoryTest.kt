package no.nav.aap.behandlingsflyt.faktagrunnlag.beregning

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.dbconnect.transaction
import no.nav.aap.behandlingsflyt.dbtest.InitTestDatabase
import no.nav.aap.behandlingsflyt.dbtestdata.ident
import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.faktagrunnlag.arbeidsevne.FakePdlGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.BeregningsgrunnlagRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.Grunnlag11_19
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.GrunnlagInntekt
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.GrunnlagUføre
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.InntektPerÅr
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
        val sak = InitTestDatabase.dataSource.transaction { sak(it) }
        val behandling = InitTestDatabase.dataSource.transaction { behandling(it, sak) }
        val inntektPerÅr = listOf(
            GrunnlagInntekt(
                år = Year.of(2015),
                inntektIKroner = Beløp(400000),
                inntektIG = GUnit(4),
                inntekt6GBegrenset = GUnit(4),
                er6GBegrenset = false
            ),
            GrunnlagInntekt(
                år = Year.of(2014),
                inntektIKroner = Beløp(400000),
                inntektIG = GUnit(4),
                inntekt6GBegrenset = GUnit(4),
                er6GBegrenset = false
            ),
            GrunnlagInntekt(
                år = Year.of(2013),
                inntektIKroner = Beløp(200000),
                inntektIG = GUnit(4),
                inntekt6GBegrenset = GUnit(4),
                er6GBegrenset = false
            )
        )

        val inntektPerÅrUføre = listOf(
            GrunnlagInntekt(
                år = Year.of(2022),
                inntektIKroner = Beløp(300000),
                inntektIG = GUnit(4),
                inntekt6GBegrenset = GUnit(4),
                er6GBegrenset = false
            ),
            GrunnlagInntekt(
                år = Year.of(2021),
                inntektIKroner = Beløp(350000),
                inntektIG = GUnit(4),
                inntekt6GBegrenset = GUnit(4),
                er6GBegrenset = false
            ),
            GrunnlagInntekt(
                år = Year.of(2020),
                inntektIKroner = Beløp(350000),
                inntektIG = GUnit(4),
                inntekt6GBegrenset = GUnit(4),
                er6GBegrenset = false
            )
        )

        val grunnlag11_19Standard = Grunnlag11_19(
            grunnlaget = GUnit(1),
            erGjennomsnitt = false,
            inntekter = inntektPerÅr
        )
        val grunnlag11_19Ytterligere = Grunnlag11_19(
            grunnlaget = GUnit(3),
            erGjennomsnitt = false,
            inntekter = inntektPerÅrUføre
        )
        val grunnlagUføre = GrunnlagUføre(
            grunnlaget = GUnit(4),
            type = GrunnlagUføre.Type.YTTERLIGERE_NEDSATT,
            grunnlag = grunnlag11_19Standard,
            grunnlagYtterligereNedsatt = grunnlag11_19Ytterligere,
            uføregrad = Prosent(50),
            //FIXME Må håndtere inntekter før oppjustering
            uføreInntekterFraForegåendeÅr = inntektPerÅrUføre.map { InntektPerÅr(it.år, it.inntektIKroner) },
            uføreInntektIKroner = Beløp(0),
            uføreYtterligereNedsattArbeidsevneÅr = Year.of(2022),
            erGjennomsnitt = false,
        )
        InitTestDatabase.dataSource.transaction { connection ->
            val beregningsgrunnlagRepository = BeregningsgrunnlagRepository(connection)

            beregningsgrunnlagRepository.lagre(behandling.id, grunnlagUføre)
        }

        InitTestDatabase.dataSource.transaction { connection ->
            val beregningsgrunnlag: GrunnlagUføre =
                BeregningsgrunnlagRepository(connection).hentHvisEksisterer(behandling.id) as GrunnlagUføre

            assertThat(beregningsgrunnlag).isEqualTo(grunnlagUføre)
            assertThat(beregningsgrunnlag.underliggende().inntekter()).isEqualTo(inntektPerÅr)
            assertThat(beregningsgrunnlag.underliggendeYtterligereNedsatt().inntekter()).isEqualTo(inntektPerÅrUføre)
            assertThat(beregningsgrunnlag).isEqualTo(grunnlagUføre)
        }
    }

    @Test
    fun `Lagre og hente opp beregningsgrunnlaget med uføre uten yrkesskade`() {
        val sak = InitTestDatabase.dataSource.transaction { sak(it) }
        val behandling = InitTestDatabase.dataSource.transaction { behandling(it, sak) }

        val grunnlag11_19Standard = Grunnlag11_19(
            grunnlaget = GUnit(1),
            erGjennomsnitt = false,
            inntekter = emptyList()
        )
        val grunnlag11_19Ytterligere = Grunnlag11_19(
            grunnlaget = GUnit(3),
            erGjennomsnitt = false,
            inntekter = emptyList()
        )
        val grunnlagUføre = GrunnlagUføre(
            grunnlaget = GUnit(4),
            type = GrunnlagUføre.Type.STANDARD,
            grunnlag = grunnlag11_19Standard,
            grunnlagYtterligereNedsatt = grunnlag11_19Ytterligere,
            uføregrad = Prosent(50),
            uføreInntekterFraForegåendeÅr = emptyList(),
            uføreInntektIKroner = Beløp(0),
            uføreYtterligereNedsattArbeidsevneÅr = Year.of(2022),
            erGjennomsnitt = false,
        )

        InitTestDatabase.dataSource.transaction { connection ->
            val beregningsgrunnlagRepository = BeregningsgrunnlagRepository(connection)

            beregningsgrunnlagRepository.lagre(behandling.id, grunnlagUføre)
        }

        InitTestDatabase.dataSource.transaction { connection ->
            val beregningsgrunnlag = BeregningsgrunnlagRepository(connection).hentHvisEksisterer(behandling.id)

            assertThat(beregningsgrunnlag).isEqualTo(grunnlagUføre)
        }
    }

    @Test
    fun `Lagre og hente opp beregningsgrunnlaget uten uføre og yrkesskade`() {
        val sak = InitTestDatabase.dataSource.transaction { sak(it) }
        val behandling = InitTestDatabase.dataSource.transaction {
            behandling(it, sak)
        }

        val grunnlag11_19Standard = Grunnlag11_19(
            grunnlaget = GUnit("1.1"),
            erGjennomsnitt = false,
            inntekter = emptyList()
        )
        InitTestDatabase.dataSource.transaction { connection ->
            val beregningsgrunnlagRepository = BeregningsgrunnlagRepository(connection)
            beregningsgrunnlagRepository.lagre(behandling.id, grunnlag11_19Standard)
        }

        InitTestDatabase.dataSource.transaction { connection ->
            val beregningsgrunnlag = BeregningsgrunnlagRepository(connection).hentHvisEksisterer(behandling.id)
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
