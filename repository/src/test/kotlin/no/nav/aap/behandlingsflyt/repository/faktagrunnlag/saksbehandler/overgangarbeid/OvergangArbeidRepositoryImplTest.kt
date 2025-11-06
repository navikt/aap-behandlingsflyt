package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.overgangarbeid

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangarbeid.OvergangArbeidVurdering
import no.nav.aap.behandlingsflyt.help.FakePdlGateway
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.overgangarbeid.OvergangArbeidRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.PersonRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.test.ident
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.time.Instant
import java.time.LocalDate

internal class OvergangArbeidRepositoryImplTest {
    companion object {
        private val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))

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
    fun `Finner ikke overgang til arbeid grunnlag hvis ikke lagret`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)

            val overgangArbeidRepository = OvergangArbeidRepositoryImpl(connection)
            val overgangArbeidGrunnlag = overgangArbeidRepository.hentHvisEksisterer(behandling.id)
            assertThat(overgangArbeidGrunnlag).isNull()
        }
    }

    @Test
    fun `lagrer og henter overgang uføre vurdering`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)

            val overgangArbeidRepository = OvergangArbeidRepositoryImpl(connection)

            val testDate = LocalDate.of(2025, 1, 1)
            val expected = OvergangArbeidVurdering(
                begrunnelse = "test",
                brukerRettPåAAP = true,
                vurderingenGjelderFra = testDate,
                vurdertAv = "Saks behandler",
                vurderingenGjelderTil = null,
                opprettet = Instant.now(),
                vurdertIBehandling = behandling.id
            )

            overgangArbeidRepository.lagre(behandling.id, listOf(expected))
            val actual = overgangArbeidRepository.hentHvisEksisterer(behandling.id)

            assertThat(actual?.vurderinger)
                .usingRecursiveComparison()
                .ignoringFields("opprettet")
                .isEqualTo(listOf(expected))
        }
    }

    @Test
    fun `test sletting`() {
        TestDataSource().use { dataSource ->
            dataSource.transaction { connection ->
                val sak = sak(connection)
                val behandling = finnEllerOpprettBehandling(connection, sak)
                val overgangArbeidRepository = OvergangArbeidRepositoryImpl(connection)
                overgangArbeidRepository.lagre(
                    behandling.id,
                    listOf(
                        OvergangArbeidVurdering(
                            begrunnelse = "test",
                            brukerRettPåAAP = true,
                            vurderingenGjelderFra = LocalDate.now(),
                            vurdertAv = "Saks behandler",
                            vurderingenGjelderTil = null,
                            opprettet = Instant.now(),
                            vurdertIBehandling = behandling.id,
                        )
                    )
                )
                overgangArbeidRepository.lagre(
                    behandling.id,
                    listOf(
                        OvergangArbeidVurdering(
                            begrunnelse = "test",
                            brukerRettPåAAP = true,
                            vurderingenGjelderFra = LocalDate.now(),
                            vurdertAv = "Saks behandler",
                            vurderingenGjelderTil = LocalDate.now().plusDays(2),
                            opprettet = Instant.now(),
                            vurdertIBehandling = behandling.id,
                        )
                    )
                )
                assertDoesNotThrow { overgangArbeidRepository.slett(behandling.id) }
            }
        }
    }

    private fun sak(connection: DBConnection): Sak {
        return PersonOgSakService(
            FakePdlGateway,
            PersonRepositoryImpl(connection),
            SakRepositoryImpl(connection)
        ).finnEllerOpprett(ident(), periode)
    }

}