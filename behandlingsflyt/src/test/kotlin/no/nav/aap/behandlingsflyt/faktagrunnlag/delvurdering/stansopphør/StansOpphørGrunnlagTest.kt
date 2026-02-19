package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.stansopphør

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate

class StansOpphørGrunnlagTest {
    @Test
    fun `returnerer gjeldende stans og opphør`() {
        val gjeldendeAnnenDato = GjeldendeStansEllerOpphør(
            dato = LocalDate.now().minusMonths(1),
            opprettet = Instant.now().minusSeconds(36000 * 60 * 30),
            vurdertIBehandling = BehandlingId(1L),
            vurdering = Opphør(
                setOf(Avslagsårsak.BRUDD_PÅ_OPPHOLDSKRAV_OPPHØR),
            )
        )
        val gjeldendeStans = GjeldendeStansEllerOpphør(
            dato = LocalDate.now(),
            opprettet = Instant.now(),
            vurdertIBehandling = BehandlingId(1L),
            vurdering = Stans(
                setOf(Avslagsårsak.BRUDD_PÅ_AKTIVITETSPLIKT_STANS),
            )
        )


        val gammeltOpphør = GjeldendeStansEllerOpphør(
            dato = LocalDate.now(),
            opprettet = Instant.now().minusSeconds(5000),
            vurdertIBehandling = BehandlingId(1L),
            vurdering = Opphør(
                setOf(Avslagsårsak.BRUDD_PÅ_AKTIVITETSPLIKT_OPPHØR),
            )
        )

        val opphevet = OpphevetStansEllerOpphør(
            dato = LocalDate.now(),
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

        assertThat(StansOpphørGrunnlag(stansOgOpphør2ForskjelligTid).gjeldendeStansOgOpphør()).isEqualTo(setOf(gjeldendeStans))


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

}