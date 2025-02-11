package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat

import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

class VilkårsresultatTest {
    @Nested
    inner class RettighetsTypeTidslinjeTest {
        @Test
        fun `tomt vilkårsresultat gir tom tidslinje`() {
            val v = Vilkårsresultat()

            assertThat(v.alle()).isEmpty()
            assertThat(v.rettighetstypeTidslinje()).isEmpty()
        }

        @Test
        fun `om sykdomsvilkåret er innvilget som student, så er rettighetstype 11-5_11-14`() {
            val v = Vilkårsresultat()
            v.leggTilHvisIkkeEksisterer(Vilkårtype.SYKDOMSVILKÅRET).leggTilVurdering(
                Vilkårsperiode(
                    Periode(LocalDate.now(), LocalDate.now().plusDays(10)),
                    utfall = Utfall.OPPFYLT,
                    begrunnelse = null,
                    innvilgelsesårsak = Innvilgelsesårsak.STUDENT,
                    faktagrunnlag = null,
                )
            )
            Vilkårtype.entries.filter { it != Vilkårtype.SYKDOMSVILKÅRET }.forEach {
                val vilkår = v.leggTilHvisIkkeEksisterer(it)
                vilkår.leggTilVurdering(
                    Vilkårsperiode(
                        Periode(LocalDate.now(), LocalDate.now().plusDays(10)),
                        utfall = Utfall.OPPFYLT,
                        begrunnelse = null,
                        faktagrunnlag = null,
                    )
                )
            }

            val tidslinje = v.rettighetstypeTidslinje()
            assertThat(tidslinje.segmenter()).hasSize(1)
            assertThat(tidslinje.segmenter().first().verdi).isEqualTo("11-5_11-14")
        }

        @Test
        fun `om sykepenger-vilkåret er oppfylt så er rettighetstype AAP-13`() {
            val v = Vilkårsresultat()
            v.leggTilHvisIkkeEksisterer(Vilkårtype.SYKEPENGEERSTATNING).leggTilVurdering(
                Vilkårsperiode(
                    Periode(LocalDate.now(), LocalDate.now().plusDays(10)),
                    utfall = Utfall.OPPFYLT,
                    begrunnelse = null,
                    faktagrunnlag = null,
                )
            )
            Vilkårtype.entries.filter { it != Vilkårtype.SYKEPENGEERSTATNING }.forEach {
                val vilkår = v.leggTilHvisIkkeEksisterer(it)
                vilkår.leggTilVurdering(
                    Vilkårsperiode(
                        Periode(LocalDate.now(), LocalDate.now().plusDays(10)),
                        utfall = Utfall.OPPFYLT,
                        begrunnelse = null,
                        faktagrunnlag = null,
                    )
                )
            }

            val tidslinje = v.rettighetstypeTidslinje()
            assertThat(tidslinje.segmenter()).hasSize(1)
            assertThat(tidslinje.segmenter().first().verdi).isEqualTo("AAP-13")
        }
    }
}