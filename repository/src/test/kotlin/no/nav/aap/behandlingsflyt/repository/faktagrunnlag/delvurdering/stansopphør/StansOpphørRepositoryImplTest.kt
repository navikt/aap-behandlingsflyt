package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.stansopphør

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.stansopphør.GjeldendeStansEllerOpphør
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.stansopphør.Opphør
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.stansopphør.Stans
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.stansopphør.StansOpphørGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.stansopphør.StansOpphørRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
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
        val b1 = behandlingsIder.next()
        val b2 = behandlingsIder.next()

        val grunnlag1 = StansOpphørGrunnlag(
            setOf(
                GjeldendeStansEllerOpphør(
                    dato = 1 januar 2020,
                    opprettet = Instant.now(),
                    vurdertIBehandling = b1,
                    vurdering = Stans(setOf(Avslagsårsak.BRUDD_PÅ_AKTIVITETSPLIKT_STANS))
                )
            )
        )

        val grunnlag2 = StansOpphørGrunnlag(
            setOf(
                GjeldendeStansEllerOpphør(
                    dato = 2 januar 2020,
                    opprettet = Instant.now(),
                    vurdertIBehandling = b2,
                    vurdering = Opphør(setOf(Avslagsårsak.BRUDD_PÅ_AKTIVITETSPLIKT_OPPHØR))
                )
            )
        )

        medRepository {
            lagre(b1, grunnlag1)
            lagre(b2, grunnlag2)

            assertThat(hentHvisEksisterer(b1)).isEqualTo(grunnlag1)
            assertThat(hentHvisEksisterer(b2)).isEqualTo(grunnlag2)
        }
    }

    private fun <R> medRepository(body: StansOpphørRepository.() -> R): R {
        return dataSource.transaction { connection ->
            StansOpphørRepositoryImpl(connection).body()
        }
    }
}