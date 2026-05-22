package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.stansopphør

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.stansopphør.GjeldendeStansEllerOpphør
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.stansopphør.IkkeStansOpphørVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.stansopphør.OpphevetStansEllerOpphør
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.stansopphør.Opphør
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.stansopphør.OpphørVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.stansopphør.Stans
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.stansopphør.StansOpphørGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.stansopphør.StansVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.help.sak
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

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

    @Test
    fun `får null hvis grunnlag mangler`() {
        dataSource.transaction { connection ->
            val behandlingId = finnEllerOpprettBehandling(connection, sak(connection)).id
            val repo = StansOpphørRepositoryImpl(connection)
            assertThat(repo.hentHvisEksisterer(behandlingId))
                .isNull()
        }
    }

    @Test
    fun `kan lese og lagre nye felter som null`() {
        dataSource.transaction { connection ->
            val behandlingId = finnEllerOpprettBehandling(connection, sak(connection)).id
            val repo = StansOpphørRepositoryImpl(connection)
            repo.lagre(behandlingId, StansOpphørGrunnlag())
            val lagretGrunnlag = repo.hentHvisEksisterer(behandlingId)!!
            assertThat(lagretGrunnlag.stansOpphørV2).isNull()
            assertThat(lagretGrunnlag.stansOpphørVurderingerV2).isNull()
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
                        fom = 1 januar 2020,
                        opprettet = Instant.now().truncatedTo(ChronoUnit.MILLIS),
                        vurdertIBehandling = b1.id,
                        vurdering = Stans(setOf(Avslagsårsak.BRUDD_PÅ_AKTIVITETSPLIKT_STANS))
                    )
                ),
                stansOpphørV2 = mapOf(
                    1 januar 2020 to Stans(setOf(Avslagsårsak.BRUDD_PÅ_AKTIVITETSPLIKT_STANS))
                ),
                stansOpphørVurderingerV2 = setOf(),
            )

            val grunnlag2 = StansOpphørGrunnlag(
                setOf(
                    GjeldendeStansEllerOpphør(
                        fom = 2 januar 2020,
                        opprettet = Instant.now().truncatedTo(ChronoUnit.MILLIS),
                        vurdertIBehandling = b2.id,
                        vurdering = Opphør(setOf(Avslagsårsak.BRUDD_PÅ_AKTIVITETSPLIKT_OPPHØR))
                    )
                ),
                stansOpphørV2 = mapOf(
                    2 januar 2020 to Opphør(setOf(Avslagsårsak.BRUDD_PÅ_AKTIVITETSPLIKT_OPPHØR))
                ),
                stansOpphørVurderingerV2 = null,
            )

            repo.lagre(b1.id, grunnlag1)
            repo.lagre(b2.id, grunnlag2)

            assertThat(repo.hentHvisEksisterer(b1.id)).isEqualTo(grunnlag1)
            assertThat(repo.hentHvisEksisterer(b2.id)).isEqualTo(grunnlag2)
        }
    }

    @Test
    fun `Overskriver med ny vurdering ved lagring`() {
        dataSource.transaction { connection ->
            val s1 = sak(connection)
            val b1 = finnEllerOpprettBehandling(connection, s1)
            val repo = StansOpphørRepositoryImpl(connection)

            val gjeldendeOpphør = StansOpphørGrunnlag(
                setOf(
                    GjeldendeStansEllerOpphør(
                        fom = 1 januar 2020,
                        opprettet = Instant.now().truncatedTo(ChronoUnit.MILLIS),
                        vurdertIBehandling = b1.id,
                        vurdering = Opphør(setOf(Avslagsårsak.BRUDD_PÅ_AKTIVITETSPLIKT_OPPHØR))
                    )
                ),
                stansOpphørV2 = mapOf(
                    1 januar 2020 to Opphør(setOf(Avslagsårsak.BRUDD_PÅ_AKTIVITETSPLIKT_OPPHØR))
                ),
                stansOpphørVurderingerV2 = null,
            )

            repo.lagre(b1.id, gjeldendeOpphør)

            assertThat(repo.hentHvisEksisterer(b1.id)).isEqualTo(gjeldendeOpphør)

            val opphevet = StansOpphørGrunnlag(
                setOf(
                    OpphevetStansEllerOpphør(
                        fom = 2 januar 2020,
                        opprettet = Instant.now().truncatedTo(ChronoUnit.MILLIS),
                        vurdertIBehandling = b1.id,
                    )
                ),
                stansOpphørV2 = mapOf(),
                stansOpphørVurderingerV2 = setOf(
                    StansVurdering(
                        fom = 2 januar 2020,
                        vurdertIBehandling = b1.id,
                        vurdertTidspunkt = Instant.now().truncatedTo(ChronoUnit.MILLIS),
                        årsaker = setOf(Avslagsårsak.BRUDD_PÅ_AKTIVITETSPLIKT_STANS),
                    ),
                    IkkeStansOpphørVurdering(
                        fom = 2 januar 2020,
                        vurdertIBehandling = b1.id,
                        vurdertTidspunkt = Instant.now().plusSeconds(5000).truncatedTo(ChronoUnit.MILLIS),
                    )
                ),
            )

            repo.lagre(b1.id, opphevet)

            assertThat(repo.hentHvisEksisterer(b1.id)).isEqualTo(opphevet)

            val gjeldendeStans = StansOpphørGrunnlag(
                setOf(
                    GjeldendeStansEllerOpphør(
                        fom = 1 januar 2020,
                        opprettet = Instant.now().truncatedTo(ChronoUnit.MILLIS),
                        vurdertIBehandling = b1.id,
                        vurdering = Stans(setOf(Avslagsårsak.BRUDD_PÅ_AKTIVITETSPLIKT_STANS))
                    )
                ),
                stansOpphørV2 = null,
                stansOpphørVurderingerV2 = null,
            )

            repo.lagre(b1.id, gjeldendeStans)

            assertThat(repo.hentHvisEksisterer(b1.id)).isEqualTo(gjeldendeStans)

            val sammensatt = StansOpphørGrunnlag(
                stansOgOpphør = gjeldendeStans.stansOgOpphør + opphevet.stansOgOpphør + gjeldendeOpphør.stansOgOpphør,
                stansOpphørV2 = null,
                stansOpphørVurderingerV2 = null,
            )

            repo.lagre(b1.id, sammensatt)

            assertThat(repo.hentHvisEksisterer(b1.id)).isEqualTo(sammensatt)
        }
    }

    @Test
    fun `kan slette aktive og ikke aktive grunnlag for behandling`() {
        dataSource.transaction { connection ->
            val s1 = sak(connection)
            val s2 = sak(connection)
            val b1 = finnEllerOpprettBehandling(connection, s1)
            val b2 = finnEllerOpprettBehandling(connection, s2)
            val repo = StansOpphørRepositoryImpl(connection)

            val vurdertTidspunkt = Instant.now().truncatedTo(ChronoUnit.MILLIS)
            val gjeldendeOpphør = StansOpphørGrunnlag(
                stansOgOpphør = setOf(
                    GjeldendeStansEllerOpphør(
                        fom = 1 januar 2020,
                        opprettet = vurdertTidspunkt.minusSeconds(5000),
                        vurdertIBehandling = b1.id,
                        vurdering = Opphør(setOf(Avslagsårsak.BRUDD_PÅ_AKTIVITETSPLIKT_OPPHØR))
                    )
                ),
                stansOpphørV2 = mapOf(
                    1 januar 2020 to Opphør(setOf(Avslagsårsak.BRUDD_PÅ_AKTIVITETSPLIKT_OPPHØR))
                ),
                stansOpphørVurderingerV2 = setOf(
                    StansVurdering(
                        fom = 1 januar 2020,
                        årsaker = setOf(Avslagsårsak.BRUDD_PÅ_AKTIVITETSPLIKT_OPPHØR),
                        vurdertIBehandling = b1.id,
                        vurdertTidspunkt = vurdertTidspunkt.minusSeconds(5000),
                    )
                ),
            )

            val gjeldendeOpphørNy = StansOpphørGrunnlag(
                stansOgOpphør = setOf(
                    GjeldendeStansEllerOpphør(
                        fom = 1 januar 2021,
                        opprettet = vurdertTidspunkt,
                        vurdertIBehandling = b1.id,
                        vurdering = Opphør(setOf(Avslagsårsak.BRUDD_PÅ_AKTIVITETSPLIKT_OPPHØR))
                    )
                ),
                stansOpphørV2 = mapOf(
                    1 januar 2021 to Opphør(setOf(Avslagsårsak.BRUDD_PÅ_AKTIVITETSPLIKT_OPPHØR))
                ),
                stansOpphørVurderingerV2 = setOf(
                    OpphørVurdering(
                        fom = 1 januar 2021,
                        årsaker = setOf(Avslagsårsak.BRUDD_PÅ_AKTIVITETSPLIKT_OPPHØR),
                        vurdertIBehandling = b1.id,
                        vurdertTidspunkt = vurdertTidspunkt,
                    )
                ),
            )

            repo.lagre(b1.id, gjeldendeOpphør)
            repo.kopier(b1.id, b2.id)


            assertThat(repo.hentHvisEksisterer(b1.id)).isEqualTo(gjeldendeOpphør)

            repo.lagre(b1.id, gjeldendeOpphørNy)
            assertThat(repo.hentHvisEksisterer(b1.id)).isEqualTo(gjeldendeOpphørNy)

            repo.slett(b1.id)
            assertThat(repo.hentHvisEksisterer(b1.id)).isNull()

            assertThat(repo.hentHvisEksisterer(b2.id)).isEqualTo(gjeldendeOpphør)
        }
    }
}