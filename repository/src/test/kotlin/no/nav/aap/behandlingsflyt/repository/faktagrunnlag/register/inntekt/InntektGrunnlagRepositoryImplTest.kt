package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.register.inntekt

import no.nav.aap.behandlingsflyt.behandling.beregning.InntektsPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.InntektPerÅr
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.help.sak
import no.nav.aap.behandlingsflyt.test.desember
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Beløp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Year

class InntektGrunnlagRepositoryImplTest {
    private lateinit var dataSource: TestDataSource

    @BeforeEach
    fun setUp() {
        dataSource = TestDataSource()
    }

    @AfterEach
    fun tearDown() {
        dataSource.close()
    }


    @Test
    fun `lagre og hente ut igjen`() {
        val behandling = dataSource.transaction {
            val sak = sak(it, Periode(1 januar 2023, 31 desember 2023))
            finnEllerOpprettBehandling(it, sak)
        }

        val inntekter: Set<InntektPerÅr> = setOf(
            InntektPerÅr(
                år = Year.of(2024),
                beløp = Beløp(123_345)
            ),
            InntektPerÅr(
                år = Year.of(2025),
                beløp = Beløp("271828.1828")
            ),
        )
        val månedligeInntekter: Set<InntektsPeriode> = setOf(
            InntektsPeriode(
                periode = Periode(1 januar 2024, 30 januar 2024),
                beløp = Beløp("123456.789")
            ),
            InntektsPeriode(
                periode = Periode(10 januar 2024, 30 januar 2026),
                beløp = Beløp("13376.789")
            ),
        )

        dataSource.transaction {
            InntektGrunnlagRepositoryImpl(it).lagre(behandling.id, inntekter, månedligeInntekter)
        }

        val uthentet = dataSource.transaction {
            InntektGrunnlagRepositoryImpl(it).hent(behandling.id)
        }

        assertThat(uthentet.inntekter).isEqualTo(inntekter)
        assertThat(uthentet.inntektPerMåned).isEqualTo(månedligeInntekter)
    }
}