package no.nav.aap.behandlingsflyt.faktagrunnlag.beregning

import no.nav.aap.behandlingsflyt.faktagrunnlag.FakePdlGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.BeregningsgrunnlagRepositoryImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.Grunnlag11_19
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.GrunnlagInntekt
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.GrunnlagUføre
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.UføreInntekt
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.repository.sak.PersonRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.test.ident
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.komponenter.verdityper.GUnit
import no.nav.aap.komponenter.verdityper.Prosent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.Year

internal class BeregningsgrunnlagRepositoryImplTest {
    private val dataSource = InitTestDatabase.freshDatabase()

    @Test
    fun `Lagre og hente opp beregningsgrunnlaget med uføre og yrkesskade`() {
        val sak = dataSource.transaction { sak(it) }
        val behandling = dataSource.transaction { finnEllerOpprettBehandling(it, sak) }
        val inntektPerÅr = listOf(
            GrunnlagInntekt(
                år = Year.of(2013),
                inntektIKroner = Beløp(400000),
                grunnbeløp = Beløp(100000),
                inntektIG = GUnit(4),
                inntekt6GBegrenset = GUnit(4),
                er6GBegrenset = false
            ),
            GrunnlagInntekt(
                år = Year.of(2014),
                inntektIKroner = Beløp(400000),
                grunnbeløp = Beløp(100000),
                inntektIG = GUnit(4),
                inntekt6GBegrenset = GUnit(4),
                er6GBegrenset = false
            ),
            GrunnlagInntekt(
                år = Year.of(2015),
                inntektIKroner = Beløp(200000),
                grunnbeløp = Beløp(100000),
                inntektIG = GUnit(2),
                inntekt6GBegrenset = GUnit(2),
                er6GBegrenset = false
            )
        )

        val inntektPerÅrUføre = listOf(
            uføreInntekt(
                år = 2020,
                uføregrad = Prosent.`50_PROSENT`,
                inntektIKroner = Beløp(300000),
                grunnbeløp = Beløp(100000),
                inntektIG = GUnit(4),
                inntekt6GBegrenset = GUnit(4),
                er6GBegrenset = false
            ),
            uføreInntekt(
                år = 2021,
                uføregrad = Prosent.`50_PROSENT`,
                inntektIKroner = Beløp(350000),
                grunnbeløp = Beløp(100000),
                inntektIG = GUnit(4),
                inntekt6GBegrenset = GUnit(4),
                er6GBegrenset = false
            ),
            uføreInntekt(
                år = 2022,
                uføregrad = Prosent.`50_PROSENT`,
                inntektIKroner = Beløp(350000),
                grunnbeløp = Beløp(100000),
                inntektIG = GUnit(4),
                inntekt6GBegrenset = GUnit(4),
                er6GBegrenset = false
            )
        )

        val grunnlag11_19Standard = Grunnlag11_19(
            grunnlaget = GUnit(1),
            erGjennomsnitt = false,
            gjennomsnittligInntektIG = GUnit(4),
            inntekter = inntektPerÅr
        )
        val grunnlag11_19Ytterligere = Grunnlag11_19(
            grunnlaget = GUnit(3),
            erGjennomsnitt = false,
            gjennomsnittligInntektIG = GUnit(4),
            inntekter = inntektPerÅrUføre.map(InntekterForUføre::grunnlagInntekt)
        )
        val grunnlagUføre = GrunnlagUføre(
            grunnlaget = GUnit(4),
            type = GrunnlagUføre.Type.YTTERLIGERE_NEDSATT,
            grunnlag = grunnlag11_19Standard,
            grunnlagYtterligereNedsatt = grunnlag11_19Ytterligere,
            uføregrad = Prosent(50),
            uføreInntekterFraForegåendeÅr = inntektPerÅrUføre.map(InntekterForUføre::uføreInntekt),
            uføreYtterligereNedsattArbeidsevneÅr = Year.of(2022)
        )
        dataSource.transaction { connection ->
            val beregningsgrunnlagRepository = BeregningsgrunnlagRepositoryImpl(connection)

            beregningsgrunnlagRepository.lagre(behandling.id, grunnlagUføre)
        }

        dataSource.transaction { connection ->
            val beregningsgrunnlag: GrunnlagUføre =
                BeregningsgrunnlagRepositoryImpl(connection).hentHvisEksisterer(behandling.id) as GrunnlagUføre

            assertThat(beregningsgrunnlag).isEqualTo(grunnlagUføre)
            assertThat(beregningsgrunnlag.underliggende().inntekter()).isEqualTo(inntektPerÅr)
            assertThat(beregningsgrunnlag.underliggendeYtterligereNedsatt().inntekter())
                .isEqualTo(inntektPerÅrUføre.map(InntekterForUføre::grunnlagInntekt))
            assertThat(beregningsgrunnlag).isEqualTo(grunnlagUføre)
        }

        // Test sletting
        dataSource.transaction { connection ->
            BeregningsgrunnlagRepositoryImpl(connection).slett(behandling.id)
        }

        dataSource.transaction { connection ->
            BeregningsgrunnlagRepositoryImpl(connection).hentHvisEksisterer(behandling.id)
                .also { assertThat(it).isNull() }
        }
    }

    @Test
    fun `Lagre og hente opp beregningsgrunnlaget med uføre uten yrkesskade`() {
        val sak = dataSource.transaction { sak(it) }
        val behandling = dataSource.transaction { finnEllerOpprettBehandling(it, sak) }

        val inntektPerÅr = listOf(
            GrunnlagInntekt(
                år = Year.of(2013),
                inntektIKroner = Beløp(400000),
                grunnbeløp = Beløp(100000),
                inntektIG = GUnit(4),
                inntekt6GBegrenset = GUnit(4),
                er6GBegrenset = false
            ),
            GrunnlagInntekt(
                år = Year.of(2014),
                inntektIKroner = Beløp(400000),
                grunnbeløp = Beløp(100000),
                inntektIG = GUnit(4),
                inntekt6GBegrenset = GUnit(4),
                er6GBegrenset = false
            ),
            GrunnlagInntekt(
                år = Year.of(2015),
                inntektIKroner = Beløp(200000),
                grunnbeløp = Beløp(100000),
                inntektIG = GUnit(2),
                inntekt6GBegrenset = GUnit(2),
                er6GBegrenset = false
            )
        )

        val grunnlag11_19Standard = Grunnlag11_19(
            grunnlaget = GUnit(1),
            erGjennomsnitt = false,
            gjennomsnittligInntektIG = GUnit(1),
            inntekter = inntektPerÅr
        )

        val inntektPerÅrUføre = listOf(
            uføreInntekt(
                år = 2020,
                uføregrad = Prosent.`50_PROSENT`,
                inntektIKroner = Beløp(300000),
                grunnbeløp = Beløp(100000),
                inntektIG = GUnit(4),
                inntekt6GBegrenset = GUnit(4),
                er6GBegrenset = false
            ),
            uføreInntekt(
                år = 2021,
                uføregrad = Prosent.`50_PROSENT`,
                inntektIKroner = Beløp(350000),
                grunnbeløp = Beløp(100000),
                inntektIG = GUnit(4),
                inntekt6GBegrenset = GUnit(4),
                er6GBegrenset = false
            ),
            uføreInntekt(
                år = 2022,
                uføregrad = Prosent.`50_PROSENT`,
                inntektIKroner = Beløp(350000),
                grunnbeløp = Beløp(100000),
                inntektIG = GUnit(4),
                inntekt6GBegrenset = GUnit(4),
                er6GBegrenset = false
            )
        )

        val grunnlag11_19Ytterligere = Grunnlag11_19(
            grunnlaget = GUnit(3),
            erGjennomsnitt = false,
            gjennomsnittligInntektIG = GUnit(3),
            inntekter = inntektPerÅrUføre.map(InntekterForUføre::grunnlagInntekt)
        )
        val grunnlagUføre = GrunnlagUføre(
            grunnlaget = GUnit(4),
            type = GrunnlagUføre.Type.STANDARD,
            grunnlag = grunnlag11_19Standard,
            grunnlagYtterligereNedsatt = grunnlag11_19Ytterligere,
            uføregrad = Prosent(50),
            uføreInntekterFraForegåendeÅr = inntektPerÅrUføre.map(InntekterForUføre::uføreInntekt),
            uføreYtterligereNedsattArbeidsevneÅr = Year.of(2022)
        )

        dataSource.transaction { connection ->
            val beregningsgrunnlagRepository = BeregningsgrunnlagRepositoryImpl(connection)

            beregningsgrunnlagRepository.lagre(behandling.id, grunnlagUføre)
        }

        dataSource.transaction { connection ->
            val beregningsgrunnlag = BeregningsgrunnlagRepositoryImpl(connection).hentHvisEksisterer(behandling.id)

            assertThat(beregningsgrunnlag).isEqualTo(grunnlagUføre)
        }
    }

    @Test
    fun `Lagre og hente opp beregningsgrunnlaget uten uføre og yrkesskade`() {
        val sak = dataSource.transaction { sak(it) }
        val behandling = dataSource.transaction {
            finnEllerOpprettBehandling(it, sak)
        }

        val grunnlag11_19Standard = Grunnlag11_19(
            grunnlaget = GUnit("1.1"),
            erGjennomsnitt = false,
            gjennomsnittligInntektIG = GUnit("1.1"),
            inntekter = emptyList()
        )
        dataSource.transaction { connection ->
            val beregningsgrunnlagRepository = BeregningsgrunnlagRepositoryImpl(connection)
            beregningsgrunnlagRepository.lagre(behandling.id, grunnlag11_19Standard)
        }

        dataSource.transaction { connection ->
            val beregningsgrunnlag = BeregningsgrunnlagRepositoryImpl(connection).hentHvisEksisterer(behandling.id)
            assertThat(beregningsgrunnlag).isEqualTo(grunnlag11_19Standard)
        }
    }

    @Test
    fun `lagre flere grunnlag`() {
        val sak = dataSource.transaction { sak(it) }
        val behandling = dataSource.transaction {
            finnEllerOpprettBehandling(it, sak)
        }

        val grunnlag11_19Standard = Grunnlag11_19(
            grunnlaget = GUnit("1.1"),
            erGjennomsnitt = false,
            gjennomsnittligInntektIG = GUnit("1.1"),
            inntekter = emptyList()
        )

        dataSource.transaction { connection ->
            val beregningsgrunnlagRepository = BeregningsgrunnlagRepositoryImpl(connection)
            beregningsgrunnlagRepository.lagre(behandling.id, grunnlag11_19Standard)
        }

        val sak2 = dataSource.transaction { sak(it) }
        val behandling2 = dataSource.transaction { finnEllerOpprettBehandling(it, sak2) }
        val inntektPerÅr = listOf(
            GrunnlagInntekt(
                år = Year.of(2013),
                inntektIKroner = Beløp(400000),
                grunnbeløp = Beløp(100000),
                inntektIG = GUnit(4),
                inntekt6GBegrenset = GUnit(4),
                er6GBegrenset = false
            ),
            GrunnlagInntekt(
                år = Year.of(2014),
                inntektIKroner = Beløp(400000),
                grunnbeløp = Beløp(100000),
                inntektIG = GUnit(4),
                inntekt6GBegrenset = GUnit(4),
                er6GBegrenset = false
            ),
            GrunnlagInntekt(
                år = Year.of(2015),
                inntektIKroner = Beløp(200000),
                grunnbeløp = Beløp(100000),
                inntektIG = GUnit(2),
                inntekt6GBegrenset = GUnit(2),
                er6GBegrenset = false
            )
        )

        val inntektPerÅrUføre = listOf(
            uføreInntekt(
                år = 2020,
                uføregrad = Prosent.`50_PROSENT`,
                inntektIKroner = Beløp(300000),
                grunnbeløp = Beløp(100000),
                inntektIG = GUnit(4),
                inntekt6GBegrenset = GUnit(4),
                er6GBegrenset = false
            ),
            uføreInntekt(
                år = 2021,
                uføregrad = Prosent.`50_PROSENT`,
                inntektIKroner = Beløp(350000),
                grunnbeløp = Beløp(100000),
                inntektIG = GUnit(4),
                inntekt6GBegrenset = GUnit(4),
                er6GBegrenset = false
            ),
            uføreInntekt(
                år = 2022,
                uføregrad = Prosent.`50_PROSENT`,
                inntektIKroner = Beløp(350000),
                grunnbeløp = Beløp(100000),
                inntektIG = GUnit(4),
                inntekt6GBegrenset = GUnit(4),
                er6GBegrenset = false
            )
        )

        val grunnlag11_19Standard_2 = Grunnlag11_19(
            grunnlaget = GUnit(1),
            erGjennomsnitt = false,
            gjennomsnittligInntektIG = GUnit(4),
            inntekter = inntektPerÅr
        )
        val grunnlag11_19Ytterligere = Grunnlag11_19(
            grunnlaget = GUnit(3),
            erGjennomsnitt = false,
            gjennomsnittligInntektIG = GUnit(4),
            inntekter = inntektPerÅrUføre.map(InntekterForUføre::grunnlagInntekt)
        )
        val grunnlagUføre = GrunnlagUføre(
            grunnlaget = GUnit(4),
            type = GrunnlagUføre.Type.YTTERLIGERE_NEDSATT,
            grunnlag = grunnlag11_19Standard_2,
            grunnlagYtterligereNedsatt = grunnlag11_19Ytterligere,
            uføregrad = Prosent(50),
            uføreInntekterFraForegåendeÅr = inntektPerÅrUføre.map(InntekterForUføre::uføreInntekt),
            uføreYtterligereNedsattArbeidsevneÅr = Year.of(2022)
        )

        dataSource.transaction { connection ->
            val beregningsgrunnlagRepository = BeregningsgrunnlagRepositoryImpl(connection)

            beregningsgrunnlagRepository.lagre(behandling2.id, grunnlagUføre)
        }

        val uthentet = dataSource.transaction {
            BeregningsgrunnlagRepositoryImpl(it).hentHvisEksisterer(behandling.id)
        }

        val uthentet2 = dataSource.transaction {
            BeregningsgrunnlagRepositoryImpl(it).hentHvisEksisterer(behandling2.id)
        }

        assertThat(uthentet).isEqualTo(grunnlag11_19Standard)
        assertThat(uthentet2).isEqualTo(grunnlagUføre)

    }

    private companion object {
        private val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))
    }

    private fun sak(connection: DBConnection): Sak {
        return PersonOgSakService(
            FakePdlGateway,
            PersonRepositoryImpl(connection),
            SakRepositoryImpl(connection)
        ).finnEllerOpprett(ident(), periode)
    }

    private class InntekterForUføre(
        val uføreInntekt: UføreInntekt,
        val grunnlagInntekt: GrunnlagInntekt
    )

    private fun uføreInntekt(
        år: Int,
        uføregrad: Prosent,
        inntektIKroner: Beløp,
        grunnbeløp: Beløp,
        inntektIG: GUnit,
        inntekt6GBegrenset: GUnit,
        er6GBegrenset: Boolean
    ): InntekterForUføre {
        return InntekterForUføre(
            UføreInntekt(
                år = Year.of(år),
                inntektIKroner = inntektIKroner.multiplisert(uføregrad.komplement()),
                uføregrad = uføregrad,
                arbeidsgrad = uføregrad.komplement(),
                inntektJustertForUføregrad = inntektIKroner,
                inntektIG = inntektIG,
                inntektIGJustertForUføregrad = inntektIG.multiplisert(uføregrad.komplement()),
                grunnbeløp = grunnbeløp
            ),
            GrunnlagInntekt(
                år = Year.of(år),
                inntektIKroner = inntektIKroner,
                grunnbeløp = grunnbeløp,
                inntektIG = inntektIG,
                inntekt6GBegrenset = inntekt6GBegrenset,
                er6GBegrenset = er6GBegrenset
            )
        )
    }
}


