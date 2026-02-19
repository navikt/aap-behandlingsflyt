package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.stansopphør

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

class StansOpphørGrunnlagTest {
    @Test
    fun `returnerer gjeldende stans og opphør`() {
        val gjeldendeAnnenDato = GjeldendeStansEllerOpphør(
            fom = LocalDate.now().minusMonths(1),
            opprettet = Instant.now().minusSeconds(36000 * 60 * 30),
            vurdertIBehandling = BehandlingId(1L),
            vurdering = Opphør(
                setOf(Avslagsårsak.BRUDD_PÅ_OPPHOLDSKRAV_OPPHØR),
            )
        )
        val gjeldendeStans = GjeldendeStansEllerOpphør(
            fom = LocalDate.now(),
            opprettet = Instant.now(),
            vurdertIBehandling = BehandlingId(1L),
            vurdering = Stans(
                setOf(Avslagsårsak.BRUDD_PÅ_AKTIVITETSPLIKT_STANS),
            )
        )


        val gammeltOpphør = GjeldendeStansEllerOpphør(
            fom = LocalDate.now(),
            opprettet = Instant.now().minusSeconds(5000),
            vurdertIBehandling = BehandlingId(1L),
            vurdering = Opphør(
                setOf(Avslagsårsak.BRUDD_PÅ_AKTIVITETSPLIKT_OPPHØR),
            )
        )

        val opphevet = OpphevetStansEllerOpphør(
            fom = LocalDate.now(),
            opprettet = Instant.now().minusSeconds(3000),
            vurdertIBehandling = BehandlingId(1L),
        )


        assertThat(StansOpphørGrunnlag(setOf(gjeldendeStans)).gjeldendeStansOgOpphør()).isEqualTo(setOf(gjeldendeStans))

        val stansOgOpphør2ForskjelligDato = setOf(
            gjeldendeAnnenDato,
            gjeldendeStans

        )

        assertThat(StansOpphørGrunnlag(stansOgOpphør2ForskjelligDato).gjeldendeStansOgOpphør()).isEqualTo(
            stansOgOpphør2ForskjelligDato
        )


        val stansOgOpphør2ForskjelligTid = setOf(
            gammeltOpphør,
            gjeldendeStans
        )

        assertThat(StansOpphørGrunnlag(stansOgOpphør2ForskjelligTid).gjeldendeStansOgOpphør()).isEqualTo(
            setOf(
                gjeldendeStans
            )
        )


        val stansOgOpphørOppheving = setOf(
            gammeltOpphør,
            opphevet
        )

        assertThat(StansOpphørGrunnlag(stansOgOpphørOppheving).gjeldendeStansOgOpphør()).isEqualTo(emptySet<GjeldendeStansEllerOpphør>())


        val stansOpphevingStans = setOf(
            gammeltOpphør,
            opphevet,
            gjeldendeStans
        )

        assertThat(StansOpphørGrunnlag(stansOpphevingStans).gjeldendeStansOgOpphør()).isEqualTo(setOf(gjeldendeStans))

    }

    @Test
    fun `legger ikke på stans og opphør ved ingen endring`() {
        val t0 = Instant.now().minusSeconds(300).truncatedTo(ChronoUnit.MILLIS)
        val t1 = t0.plusSeconds(300)
        val stansOpphørGrunnlag = StansOpphørGrunnlag(
            emptySet()
        )
        val utledetStansOgOpphør = mapOf(
            LocalDate.now() to Stans(
                setOf(Avslagsårsak.BRUDD_PÅ_AKTIVITETSPLIKT_STANS),
            )
        )
        val oppdatertGrunnlag = stansOpphørGrunnlag.utledNyttGrunnlag(
            utledetStansOgOpphør,
            BehandlingId(0L),
            Clock.fixed(t0, ZoneId.systemDefault())
        )

        assertThat(oppdatertGrunnlag).isEqualTo(
            StansOpphørGrunnlag(
                setOf(
                    GjeldendeStansEllerOpphør(
                        fom = LocalDate.now(),
                        opprettet = t0,
                        vurdertIBehandling = BehandlingId(0L),
                        vurdering = Stans(
                            setOf(Avslagsårsak.BRUDD_PÅ_AKTIVITETSPLIKT_STANS),
                        )
                    )
                )
            )
        )

        val rekjørtGrunnlag = oppdatertGrunnlag.utledNyttGrunnlag(
            utledetStansOgOpphør,
            BehandlingId(2L),
            Clock.fixed(t1, ZoneId.systemDefault())
        )

        assertThat(rekjørtGrunnlag).isEqualTo(
            StansOpphørGrunnlag(
                setOf(
                    GjeldendeStansEllerOpphør(
                        fom = LocalDate.now(),
                        opprettet = t0,
                        vurdertIBehandling = BehandlingId(0L),
                        vurdering = Stans(
                            setOf(Avslagsårsak.BRUDD_PÅ_AKTIVITETSPLIKT_STANS),
                        )
                    )
                )
            )
        )
    }

    @Test
    fun `legger på opphevet ved fjerning`() {
        val t0 = Instant.now().minusSeconds(300).truncatedTo(ChronoUnit.MILLIS)
        val t1 = t0.plusSeconds(300)
        val stansOpphørGrunnlag = StansOpphørGrunnlag(
            emptySet()
        )
        val utledetStansOgOpphør = mapOf(
            LocalDate.now() to Stans(
                setOf(Avslagsårsak.BRUDD_PÅ_AKTIVITETSPLIKT_STANS),
            )
        )

        val grunnlag = stansOpphørGrunnlag.utledNyttGrunnlag(
            utledetStansOgOpphør,
            BehandlingId(0L),
            Clock.fixed(t0, ZoneId.systemDefault())
        )

        val oppdatertGrunnlag =
            grunnlag.utledNyttGrunnlag(emptyMap(), BehandlingId(1L), Clock.fixed(t1, ZoneId.systemDefault()))

        assertThat(oppdatertGrunnlag).isEqualTo(
            StansOpphørGrunnlag(
                setOf(
                    GjeldendeStansEllerOpphør(
                        fom = LocalDate.now(),
                        opprettet = t0,
                        vurdertIBehandling = BehandlingId(0L),
                        vurdering = Stans(
                            setOf(Avslagsårsak.BRUDD_PÅ_AKTIVITETSPLIKT_STANS),
                        )
                    ),
                    OpphevetStansEllerOpphør(
                        fom = LocalDate.now(),
                        opprettet = t1,
                        vurdertIBehandling = BehandlingId(1L),
                    )
                )
            )
        )
    }

    @Test
    fun `endrer stans til opphør`() {
        val t0 = Instant.now().minusSeconds(300).truncatedTo(ChronoUnit.MILLIS)
        val t1 = t0.plusSeconds(300)
        val stansOpphørGrunnlag = StansOpphørGrunnlag(
            emptySet()
        )
        val utledetStansOgOpphør = mapOf(
            LocalDate.now() to Stans(
                setOf(Avslagsårsak.BRUDD_PÅ_AKTIVITETSPLIKT_STANS),
            )
        )

        val grunnlag = stansOpphørGrunnlag.utledNyttGrunnlag(
            utledetStansOgOpphør,
            BehandlingId(0L),
            Clock.fixed(t0, ZoneId.systemDefault())
        )

        val oppdatertGrunnlag = grunnlag.utledNyttGrunnlag(
            mapOf(
                LocalDate.now() to Opphør(
                    setOf(Avslagsårsak.BRUDD_PÅ_AKTIVITETSPLIKT_OPPHØR)
                )
            ), BehandlingId(1L), Clock.fixed(
                t1, ZoneId.systemDefault()
            )
        )


        assertThat(oppdatertGrunnlag).isEqualTo(
            StansOpphørGrunnlag(
                setOf(
                    GjeldendeStansEllerOpphør(
                        fom = LocalDate.now(),
                        opprettet = t0,
                        vurdertIBehandling = BehandlingId(0L),
                        vurdering = Stans(
                            setOf(Avslagsårsak.BRUDD_PÅ_AKTIVITETSPLIKT_STANS),
                        )
                    ),
                    GjeldendeStansEllerOpphør(
                        fom = LocalDate.now(),
                        opprettet = t1,
                        vurdertIBehandling = BehandlingId(1L),
                        vurdering = Opphør(
                            setOf(Avslagsårsak.BRUDD_PÅ_AKTIVITETSPLIKT_OPPHØR)
                        )
                    )
                )
            )
        )
    }
}