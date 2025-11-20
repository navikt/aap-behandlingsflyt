package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.barnetillegg

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.barnetillegg.BarnetilleggPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.BarnIdentifikator
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.help.sak
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.barnetillegg.BarnetilleggRepositoryImpl
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.time.LocalDate

internal class BarnetilleggRepositoryImplTest {
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
    fun `Finner ikke barnetilleggGrunnlag hvis ikke lagret`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)

            val barnetilleggRepository = BarnetilleggRepositoryImpl(connection)
            val barnetilleggGrunnlag = barnetilleggRepository.hentHvisEksisterer(behandling.id)
            assertThat(barnetilleggGrunnlag).isNull()
        }
    }

    @Test
    fun `Lagrer og henter barnetilleggGrunnlag`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)

            val barnetilleggRepository = BarnetilleggRepositoryImpl(connection)
            val barnetilleggPeriode = listOf(
                BarnetilleggPeriode(
                    Periode(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 1)),
                    setOf(BarnIdentifikator.BarnIdent("12345678910"), BarnIdentifikator.BarnIdent("12345678911"))
                ),
                BarnetilleggPeriode(
                    Periode(LocalDate.of(2024, 1, 2), LocalDate.of(2024, 1, 2)),
                    setOf(BarnIdentifikator.BarnIdent("12345678910"))
                )
            )

            barnetilleggRepository.lagre(behandling.id, barnetilleggPeriode)

            val barnetilleggGrunnlag = barnetilleggRepository.hentHvisEksisterer(
                behandling.id
            )

            assertThat(barnetilleggGrunnlag?.perioder).isEqualTo(
                barnetilleggPeriode
            )
        }
    }

    @Test
    fun `lager nytt deaktiverer og lager nytt grunnlag ved ny lagring`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)

            val barnetilleggRepository = BarnetilleggRepositoryImpl(connection)
            val barnetilleggPeriode1 = BarnetilleggPeriode(
                Periode(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 1)),
                setOf(BarnIdentifikator.BarnIdent("12345678910"), BarnIdentifikator.BarnIdent("12345678911"))
            )


            val barnetilleggPeriode2 = BarnetilleggPeriode(
                Periode(LocalDate.of(2024, 1, 2), LocalDate.of(2024, 1, 2)),
                setOf(BarnIdentifikator.BarnIdent("12345678910"))
            )


            barnetilleggRepository.lagre(behandling.id, listOf(barnetilleggPeriode1))
            barnetilleggRepository.lagre(behandling.id, listOf(barnetilleggPeriode2))

            val barnetilleggGrunnlag = barnetilleggRepository.hentHvisEksisterer(
                behandling.id
            )

            assertThat(barnetilleggGrunnlag?.perioder).isEqualTo(
                listOf(barnetilleggPeriode2)
            )
        }
    }

    @Test
    fun `Kopierer barnetilleggGrunnlag fra en behandling til en annen`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling1 = finnEllerOpprettBehandling(connection, sak)
            val barnetilleggRepository = BarnetilleggRepositoryImpl(connection)
            barnetilleggRepository.lagre(
                behandling1.id,
                listOf(
                    BarnetilleggPeriode(
                        Periode(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 1)),
                        setOf(BarnIdentifikator.BarnIdent("12345678910"), BarnIdentifikator.BarnIdent("12345678911"))
                    ),
                )
            )
            BehandlingRepositoryImpl(connection).oppdaterBehandlingStatus(behandling1.id, Status.AVSLUTTET)
            val behandling2 = finnEllerOpprettBehandling(connection, sak)

            val barnetilleggGrunnlag = barnetilleggRepository.hentHvisEksisterer(behandling2.id)
            assertThat(barnetilleggGrunnlag?.perioder)
                .containsExactly(
                    BarnetilleggPeriode(
                        Periode(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 1)),
                        setOf(BarnIdentifikator.BarnIdent("12345678910"), BarnIdentifikator.BarnIdent("12345678911"))
                    ),
                )
        }
    }

    @Test
    fun `test sletting`() {
        // FIXME ny db trengs her?
        TestDataSource().use { dataSource ->
            dataSource.transaction { connection ->
                val sak = sak(connection)
                val behandling = finnEllerOpprettBehandling(connection, sak)
                val barnetilleggRepository = BarnetilleggRepositoryImpl(connection)
                barnetilleggRepository.lagre(
                    behandling.id, listOf(
                        BarnetilleggPeriode(
                            Periode(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 1).plusYears(18)),
                            setOf(BarnIdentifikator.BarnIdent("12345678910"))
                        )
                    )
                )
                barnetilleggRepository.lagre(
                    behandling.id, listOf(
                        BarnetilleggPeriode(
                            Periode(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 1).plusYears(18)),
                            setOf(BarnIdentifikator.BarnIdent("12345678910"))
                        )
                    )
                )
                assertDoesNotThrow {
                    barnetilleggRepository.slett(behandling.id)
                }
            }
        }
    }
}