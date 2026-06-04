package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.samordning

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.uførevurdering.SamordningUføreVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.uførevurdering.SamordningUføreVurderingPeriode
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.help.sak
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.komponenter.verdityper.Prosent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.time.LocalDate
import java.time.LocalDateTime

internal class SamordningUføreRepositoryImplTest {
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


    private val virkningstidspunkt = LocalDate.now()

    @Test
    fun `skal lagre ned en helt ny vurdering og hente den opp igjen`() {
        val behandling = dataSource.transaction {
            finnEllerOpprettBehandling(it, sak(it, virkningstidspunkt))
        }

        // Lagre ytelse
        val vurdering = SamordningUføreVurdering(
            begrunnelse = "En fin begrunnelse", vurderingPerioder = listOf(
                SamordningUføreVurderingPeriode(
                    virkningstidspunkt = virkningstidspunkt,
                    uføregradTilSamordning = Prosent.`50_PROSENT`
                ),
                SamordningUføreVurderingPeriode(
                    virkningstidspunkt = virkningstidspunkt.plusMonths(4),
                    uføregradTilSamordning = Prosent.`70_PROSENT`
                )
            ),
            vurdertAv = "ident",
            vurdertTidspunkt = LocalDateTime.now()
        )
        dataSource.transaction {
            SamordningUføreRepositoryImpl(it).lagre(behandling.id, vurdering)
        }

        val uthentet = dataSource.transaction {
            SamordningUføreRepositoryImpl(it).hentHvisEksisterer(behandling.id)
        }

        assertThat(uthentet).isNotNull
        assertThat(uthentet?.vurdering).usingRecursiveComparison().ignoringFields("vurdertTidspunkt").isEqualTo(vurdering)
    }

    @Test
    fun `test sletting`() {
        TestDataSource().use { dataSource ->
            dataSource.transaction { connection ->
                val sak = sak(connection)
                val behandling = finnEllerOpprettBehandling(connection, sak)
                val samordningUføreRepository = SamordningUføreRepositoryImpl(connection)
                samordningUføreRepository.lagre(
                    behandling.id,
                    SamordningUføreVurdering(
                        begrunnelse = "En fin begrunnelse", vurderingPerioder = listOf(
                            SamordningUføreVurderingPeriode(
                                virkningstidspunkt = virkningstidspunkt,
                                uføregradTilSamordning = Prosent.`50_PROSENT`
                            ),
                            SamordningUføreVurderingPeriode(
                                virkningstidspunkt = virkningstidspunkt.plusMonths(4),
                                uføregradTilSamordning = Prosent.`70_PROSENT`
                            )
                        ),
                        vurdertAv = "ident",
                        vurdertTidspunkt = LocalDateTime.now()
                    )
                )
                samordningUføreRepository.lagre(
                    behandling.id,
                    SamordningUføreVurdering(
                        begrunnelse = "En fin begrunnelse", vurderingPerioder = listOf(
                            SamordningUføreVurderingPeriode(
                                virkningstidspunkt = virkningstidspunkt,
                                uføregradTilSamordning = Prosent.`50_PROSENT`
                            ),
                            SamordningUføreVurderingPeriode(
                                virkningstidspunkt = virkningstidspunkt.plusMonths(2),
                                uføregradTilSamordning = Prosent.`70_PROSENT`
                            )
                        ),
                        vurdertAv = "ident",
                        vurdertTidspunkt = LocalDateTime.now()
                    )
                )
                assertDoesNotThrow {
                    samordningUføreRepository.slett(behandling.id)
                }

            }
        }
    }
}
