package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.register.dagpenger

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.andrestatligeytelservurdering.gateway.DagpengerPeriode
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.help.sak
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class DagpengerRepositoryImplTest {

    private companion object {
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
    fun `lagrer og henter perioder med innvilget dagpenger`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)

            val dagpengerRepo = DagpengerRepositoryImpl(connection = connection)

            val dagpengerList: List<DagpengerPeriode> =
                listOf(
                    DagpengerPeriode(
                        periode = no.nav.aap.komponenter.type.Periode(
                            fom = java.time.LocalDate.of(2023,1,1),
                            tom = java.time.LocalDate.of(2023,3,31)
                        ),
                        kilde = no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.andrestatligeytelservurdering.gateway.DagpengerKilde.ARENA,
                        dagpengerYtelseType = no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.andrestatligeytelservurdering.gateway.DagpengerYtelseType.DAGPENGER_ARBEIDSSOKER_ORDINAER
                    ),
                    DagpengerPeriode(
                        periode = no.nav.aap.komponenter.type.Periode(
                            fom = java.time.LocalDate.of(2023,4,1),
                            tom = java.time.LocalDate.of(2023,6,30)
                        ),
                        kilde = no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.andrestatligeytelservurdering.gateway.DagpengerKilde.DP_SAK,
                        dagpengerYtelseType = no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.andrestatligeytelservurdering.gateway.DagpengerYtelseType.DAGPENGER_PERMITTERING_ORDINAER
                    )
                )

            dagpengerRepo.lagre(behandling.id, dagpengerList)

            assertThat(dagpengerRepo.hent(behandling.id)).isEqualTo(dagpengerList)

        }

    }


    @Test
    fun `ingen perioder returnerer en tom liste`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)

            val dagpengerRepo = DagpengerRepositoryImpl(connection = connection)

            assertThat(dagpengerRepo.hent(behandling.id)).isEmpty()

        }
    }


    @Test
    fun kopier() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandlingFra = finnEllerOpprettBehandling(connection, sak)

            val dagpengerRepo = DagpengerRepositoryImpl(connection = connection)

            val dagpengerList: List<DagpengerPeriode> =
                listOf(
                    DagpengerPeriode(
                        periode = no.nav.aap.komponenter.type.Periode(
                            fom = java.time.LocalDate.of(2023,1,1),
                            tom = java.time.LocalDate.of(2023,3,31)
                        ),
                        kilde = no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.andrestatligeytelservurdering.gateway.DagpengerKilde.ARENA,
                        dagpengerYtelseType = no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.andrestatligeytelservurdering.gateway.DagpengerYtelseType.DAGPENGER_ARBEIDSSOKER_ORDINAER
                    )
                )

            dagpengerRepo.lagre(behandlingFra.id, dagpengerList)
            BehandlingRepositoryImpl(connection).oppdaterBehandlingStatus(behandlingFra.id, Status.AVSLUTTET)

            val behandlingTil = finnEllerOpprettBehandling(connection, sak)

            dagpengerRepo.kopier(fraBehandling = behandlingFra.id, tilBehandling = behandlingTil.id)

            assertThat(dagpengerRepo.hent(behandlingTil.id)).isEqualTo(dagpengerList)
        }
    }

}