package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.stansopphør

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.stansopphør.GjeldendeStansEllerOpphør
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.stansopphør.Opphør
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.stansopphør.Stans
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.stansopphør.StansOpphørGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.stansopphør.StansOpphørRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.help.sak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.time.Instant

class StansOpphørRepositoryImplTest {
    companion object {
        private lateinit var dataSource: TestDataSource

        @BeforeAll
        @JvmStatic
        fun beforeAll() {
            dataSource = TestDataSource()
        }

        @AfterAll
        @JvmStatic
        fun afterAll() = dataSource.close()
    }

    private val behandlingsIder = generateSequence(0L) { it + 1 }.map(::BehandlingId).iterator()

    @Test
    fun `får null hvis grunnlag mangler`() {
        val behandlingsId = behandlingsIder.next()

        medRepository {
            assertThat(hentHvisEksisterer(behandlingsId))
                .isNull()
        }
    }

    @Test
    fun `får tilbake det man skriver`() {
        dataSource.transaction { connection ->
            val s1 = sak(connection)
            val s2 = sak(connection)
            val b1 = finnEllerOpprettBehandling(connection, s1)
            val b2 = finnEllerOpprettBehandling(connection, s2)
            val repo = StansOpphørRepositoryImpl(connection)

            val grunnlag1 = StansOpphørGrunnlag(
                setOf(
                    GjeldendeStansEllerOpphør(
                        dato = 1 januar 2020,
                        opprettet = Instant.now(),
                        vurdertIBehandling = b1.id,
                        vurdering = Stans(setOf(Avslagsårsak.BRUDD_PÅ_AKTIVITETSPLIKT_STANS))
                    )
                )
            )

            val grunnlag2 = StansOpphørGrunnlag(
                setOf(
                    GjeldendeStansEllerOpphør(
                        dato = 2 januar 2020,
                        opprettet = Instant.now(),
                        vurdertIBehandling = b2.id,
                        vurdering = Opphør(setOf(Avslagsårsak.BRUDD_PÅ_AKTIVITETSPLIKT_OPPHØR))
                    )
                )
            )

                repo.lagre(b1.id, grunnlag1)
                repo.lagre(b2.id, grunnlag2)

                assertThat(repo.hentHvisEksisterer(b1.id)).isEqualTo(grunnlag1)
                assertThat(repo.hentHvisEksisterer(b2.id)).isEqualTo(grunnlag2)

        }
    }

    private fun <R> medRepository(body: StansOpphørRepository.() -> R): R {
        return dataSource.transaction { connection ->
            StansOpphørRepositoryImpl(connection).body()
        }
    }
}