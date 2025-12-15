package no.nav.aap.behandlingsflyt.repository.sak

import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.BeregnTilkjentYtelseService
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.TilkjentYtelseGrunnlag
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.TilkjentYtelsePeriode
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Kvote
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.MeldepliktStatus
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.barnetillegg.BarnetilleggGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.barnetillegg.BarnetilleggPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.SamordningGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.arbeidsgiver.SamordningArbeidsgiverGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.arbeidsgiver.SamordningArbeidsgiverVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.uførevurdering.SamordningUføreGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.uførevurdering.SamordningUføreVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.ArbeidsGradering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.Underveisperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.BarnIdentifikator
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.help.sak
import no.nav.aap.behandlingsflyt.repository.behandling.tilkjentytelse.TilkjentYtelseRepositoryImpl
import no.nav.aap.behandlingsflyt.test.desember
import no.nav.aap.behandlingsflyt.test.februar
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Dagsatser
import no.nav.aap.komponenter.verdityper.GUnit
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.komponenter.verdityper.TimerArbeid
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class SakRepositoryImplTest {
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
    fun `teste å hente ut sak(er) med barnetillegg`() {
        val periode = Periode(1 desember 2025, 1 februar 2026)
        val (sak, behandling) = dataSource.transaction {
            val sak = sak(it, periode)

            val b = finnEllerOpprettBehandling(it, sak)

            Pair(sak, b)
        }

        val grunnlag = TilkjentYtelseGrunnlag(
            fødselsdato = Fødselsdato(12 januar 1990),
            beregningsgrunnlag = GUnit("0.0078500000"),
            underveisgrunnlag = UnderveisGrunnlag(
                id = 1L, perioder = listOf(
                    Underveisperiode(
                        periode = periode,
                        meldePeriode = periode,
                        utfall = Utfall.OPPFYLT,
                        rettighetsType = RettighetsType.BISTANDSBEHOV,
                        avslagsårsak = null,
                        grenseverdi = Prosent.`100_PROSENT`,
                        arbeidsgradering = ArbeidsGradering(
                            totaltAntallTimer = TimerArbeid(BigDecimal(10)),
                            andelArbeid = Prosent.`50_PROSENT`,
                            fastsattArbeidsevne = Prosent.`50_PROSENT`,
                            gradering = Prosent.`50_PROSENT`,
                            opplysningerMottatt = 1 desember 2025,
                        ),
                        trekk = Dagsatser(0),
                        brukerAvKvoter = setOf(Kvote.ORDINÆR),
                        institusjonsoppholdReduksjon = Prosent.`0_PROSENT`,
                        meldepliktStatus = MeldepliktStatus.MELDT_SEG,
                        meldepliktGradering = Prosent.`0_PROSENT`,
                    )
                )
            ),
            barnetilleggGrunnlag = BarnetilleggGrunnlag(
                listOf(BarnetilleggPeriode(
                    periode = periode,
                    personIdenter = setOf(BarnIdentifikator.BarnIdent("dccc"))
                ))
            ),
            samordningGrunnlag = SamordningGrunnlag(emptySet()),
            samordningUføre = SamordningUføreGrunnlag(vurdering = SamordningUføreVurdering("", emptyList(), "ident")),
            samordningArbeidsgiver = SamordningArbeidsgiverGrunnlag(
                vurdering = SamordningArbeidsgiverVurdering(
                    "",
                    listOf(Periode(LocalDate.now(), LocalDate.now())), vurdertAv = "ident"
                )
            ),
            minsteÅrligeYtelse = Tidslinje.empty()
        )

        val tidslinje = BeregnTilkjentYtelseService(grunnlag).beregnTilkjentYtelse()

        dataSource.transaction {
            TilkjentYtelseRepositoryImpl(it).lagre(
                behandling.id,
                tidslinje.segmenter().map { (periode, tilkjent) -> TilkjentYtelsePeriode(periode, tilkjent) },
                grunnlag,
                "c"
            )
        }

        val saker = dataSource.transaction {
            SakRepositoryImpl(it).finnSakerMedBarnetillegg(1 januar 2026)
        }

        assertThat(saker).hasSize(1)
    }

}