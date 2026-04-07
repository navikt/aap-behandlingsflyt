package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.register.tiltakspenger

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.andrestatligeytelservurdering.gateway.TiltakspengerKilde
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.andrestatligeytelservurdering.gateway.TiltakspengerPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.andrestatligeytelservurdering.gateway.TiltakspengerYtelseType
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
import java.time.LocalDate

class TiltakspengerRepositoryImplTest {

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
    fun `lagrer og henter perioder med innvilget tiltakspenger`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)

            val tiltakspengerRepo = TiltakspengerRepositoryImpl(connection = connection)

            val tiltakspengerList: List<TiltakspengerPeriode> =
                listOf(
                    TiltakspengerPeriode(
                        fraOgMed = LocalDate.of(2023,1,1),
                        tilOgMed = LocalDate.of(2023,3,31),
                        kilde = TiltakspengerKilde.ARENA,
                        tiltakspengerYtelseType = TiltakspengerYtelseType.TILTAKSPENGER
                    ),
                    TiltakspengerPeriode(
                        fraOgMed = LocalDate.of(2023,4,1),
                        tilOgMed = LocalDate.of(2023,6,30),
                        kilde = TiltakspengerKilde.TPSAK,
                        tiltakspengerYtelseType = TiltakspengerYtelseType.TILTAKSPENGER
                    )
                )

            // Act
            tiltakspengerRepo.lagre(behandling.id, tiltakspengerList)

            // Assert
            assertThat(tiltakspengerRepo.hent(behandling.id)).isEqualTo(tiltakspengerList)
        }
    }


    @Test
    fun `ingen perioder returnerer en tom liste`() {
        dataSource.transaction { connection ->
            // Assemble
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)

            val tiltakspengerRepo = TiltakspengerRepositoryImpl(connection = connection)

            // Act & Assert
            assertThat(tiltakspengerRepo.hent(behandling.id)).isEmpty()
        }
    }


    @Test
    fun kopier() {
        dataSource.transaction { connection ->
            // Assemble
            val sak = sak(connection)
            val behandlingFra = finnEllerOpprettBehandling(connection, sak)

            val tiltakspengerRepo = TiltakspengerRepositoryImpl(connection = connection)

            val tiltakspengerList: List<TiltakspengerPeriode> =
                listOf(
                    TiltakspengerPeriode(
                        fraOgMed = LocalDate.of(2023,1,1),
                        tilOgMed = LocalDate.of(2023,3,31),
                        kilde = TiltakspengerKilde.ARENA,
                        tiltakspengerYtelseType = TiltakspengerYtelseType.TILTAKSPENGER
                    )
                )

            tiltakspengerRepo.lagre(behandlingFra.id, tiltakspengerList)
            BehandlingRepositoryImpl(connection).oppdaterBehandlingStatus(behandlingFra.id, Status.AVSLUTTET)

            val behandlingTil = finnEllerOpprettBehandling(connection, sak)

            // Act
            tiltakspengerRepo.kopier(fraBehandling = behandlingFra.id, tilBehandling = behandlingTil.id)

            // Assert
            assertThat(tiltakspengerRepo.hent(behandlingTil.id)).isEqualTo(tiltakspengerList)
        }
    }

}