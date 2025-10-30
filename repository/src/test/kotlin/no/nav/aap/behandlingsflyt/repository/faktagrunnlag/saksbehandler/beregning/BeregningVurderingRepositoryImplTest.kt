package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.beregning

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningstidspunktVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.YrkesskadeBeløpVurdering
import no.nav.aap.behandlingsflyt.help.FakePdlGateway
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.repository.sak.PersonRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.test.ident
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Beløp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AutoClose
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal
import java.time.LocalDate
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BeregningVurderingRepositoryImplTest {

    @AutoClose
    private val dataSource = TestDataSource()

    @Test
    fun `lagre, hent ut igjen, og slett`() {
        val tidspunktVurdering = BeregningstidspunktVurdering(
            begrunnelse = "Dette er en begrunnelse",
            nedsattArbeidsevneDato = LocalDate.now(),
            ytterligereNedsattBegrunnelse = "Dette er en ytterligere begrunnelse",
            ytterligereNedsattArbeidsevneDato = LocalDate.now().plusDays(30),
            vurdertAv = "saksbehandler"
        )

        val yrkesskadeVurderinger = listOf(
            YrkesskadeBeløpVurdering(
                antattÅrligInntekt = Beløp(BigDecimal("450000")),
                referanse = "Referanse 1",
                begrunnelse = "Begrunnelse for yrkesskade 1",
                vurdertAv = "saksbehandler"
            ),
            YrkesskadeBeløpVurdering(
                antattÅrligInntekt = Beløp(BigDecimal("500000")),
                referanse = "Referanse 2",
                begrunnelse = "Begrunnelse for yrkesskade 2",
                vurdertAv = "saksbehandler"
            )
        )

        val sak = dataSource.transaction { sak(it) }
        val behandling = dataSource.transaction { connection ->
            val behandling = finnEllerOpprettBehandling(connection, sak)
            val beregningVurderingRepository = BeregningVurderingRepositoryImpl(connection)

            // Lagre tidspunktVurdering
            beregningVurderingRepository.lagre(behandling.id, tidspunktVurdering)

            // Lagre yrkesskadeVurderinger
            beregningVurderingRepository.lagre(behandling.id, yrkesskadeVurderinger)

            behandling
        }

        dataSource.transaction { connection ->
            val beregningVurderingRepository = BeregningVurderingRepositoryImpl(connection)

            // Hent og verifiser
            val beregningGrunnlag = beregningVurderingRepository.hent(behandling.id)

            // Verifiser tidspunktVurdering
            assertThat(beregningGrunnlag.tidspunktVurdering).isNotNull
                .extracting(
                    { it!!.begrunnelse },
                    { it!!.nedsattArbeidsevneDato },
                    { it!!.ytterligereNedsattBegrunnelse },
                    { it!!.ytterligereNedsattArbeidsevneDato }
                )
                .containsExactly(
                    tidspunktVurdering.begrunnelse,
                    tidspunktVurdering.nedsattArbeidsevneDato,
                    tidspunktVurdering.ytterligereNedsattBegrunnelse,
                    tidspunktVurdering.ytterligereNedsattArbeidsevneDato
                )

            // Verifiser yrkesskadeVurderinger
            assertThat(beregningGrunnlag.yrkesskadeBeløpVurdering?.vurderinger?.size).isEqualTo(yrkesskadeVurderinger.size)

            // Verifiser at alle vurderinger finnes, uavhengig av rekkefølge
            val hentedeVurderinger = beregningGrunnlag.yrkesskadeBeløpVurdering?.vurderinger
            assertThat(hentedeVurderinger)
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("vurdertTidspunkt")
                .containsExactlyInAnyOrderElementsOf(yrkesskadeVurderinger)

            // Slett og verifiser at det er slettet
            beregningVurderingRepository.slett(behandling.id)
            assertThat(beregningVurderingRepository.hentHvisEksisterer(behandling.id)).isNull()
        }
    }

    @ParameterizedTest
    @MethodSource("testDataAvrunding")
    fun `Skal avrunde opp til nærmeste tusen`(beløp: Int, avrundetBeløp: Int) {
        val yrkesskadeVurderinger = listOf(
            YrkesskadeBeløpVurdering(
                antattÅrligInntekt = Beløp(beløp),
                referanse = "Referanse 1",
                begrunnelse = "Begrunnelse for yrkesskade 1",
                vurdertAv = "saksbehandler"
            )
        )

        val sak = dataSource.transaction { sak(it) }
        val behandling = dataSource.transaction { connection ->
            val behandling = finnEllerOpprettBehandling(connection, sak)
            val beregningVurderingRepository = BeregningVurderingRepositoryImpl(connection)

            // Lagre yrkesskadeVurderinger
            beregningVurderingRepository.lagre(behandling.id, yrkesskadeVurderinger)

            behandling
        }

        dataSource.transaction { connection ->
            val beregningVurderingRepository = BeregningVurderingRepositoryImpl(connection)

            // Hent og verifiser
            val beregningGrunnlag = beregningVurderingRepository.hent(behandling.id)



            // Verifiser yrkesskadeVurderinger
            assertThat(beregningGrunnlag.yrkesskadeBeløpVurdering?.vurderinger?.size).isEqualTo(yrkesskadeVurderinger.size)

            // Verifiser at antattÅrligInntekt er riktig avrundet
            val hentedeVurderinger = beregningGrunnlag.yrkesskadeBeløpVurdering?.vurderinger
            assertThat(hentedeVurderinger).isNotNull()
            assertThat(hentedeVurderinger!![0].antattÅrligInntekt).isEqualTo(Beløp(avrundetBeløp))

        }
    }


    private companion object {
        private val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))

        @JvmStatic
        fun testDataAvrunding(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(500001, 501000),
                Arguments.of(499999, 500000),
                Arguments.of(367000, 367000),
            )
        }

    }

    private fun sak(connection: DBConnection): Sak {
        return PersonOgSakService(
            FakePdlGateway,
            PersonRepositoryImpl(connection),
            SakRepositoryImpl(connection)
        ).finnEllerOpprett(
            ident(),
            periode
        )
    }
}
