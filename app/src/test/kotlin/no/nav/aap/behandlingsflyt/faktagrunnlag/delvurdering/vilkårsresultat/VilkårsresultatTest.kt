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
            val periode = Periode(LocalDate.now(), LocalDate.now().plusDays(10))
            v.leggTilHvisIkkeEksisterer(Vilkårtype.BISTANDSVILKÅRET).leggTilVurdering(
                Vilkårsperiode(
                    periode,
                    utfall = Utfall.OPPFYLT,
                    begrunnelse = null,
                    innvilgelsesårsak = Innvilgelsesårsak.STUDENT,
                )
            )
            Vilkårtype.entries.filter { it != Vilkårtype.BISTANDSVILKÅRET }.forEach {
                val vilkår = v.leggTilHvisIkkeEksisterer(it)
                vilkår.leggTilVurdering(
                    Vilkårsperiode(
                        periode,
                        utfall = Utfall.OPPFYLT,
                        begrunnelse = null,
                    )
                )
            }

            val tidslinje = v.rettighetstypeTidslinje()
            assertThat(tidslinje.segmenter()).hasSize(1)
            assertThat(tidslinje.segmenter().first().verdi).isEqualTo(RettighetsType.STUDENT)
            assertThat(tidslinje.helePerioden()).isEqualTo(periode)
        }

        @Test
        fun `om sykepenger-vilkåret er oppfylt så er rettighetstype AAP-13`() {
            val v = Vilkårsresultat()
            val periode = Periode(LocalDate.now(), LocalDate.now().plusDays(10))
            v.leggTilHvisIkkeEksisterer(Vilkårtype.SYKEPENGEERSTATNING).leggTilVurdering(
                Vilkårsperiode(
                    periode,
                    utfall = Utfall.OPPFYLT,
                    begrunnelse = null,
                )
            )
            Vilkårtype.entries.filter { it != Vilkårtype.SYKEPENGEERSTATNING }.forEach {
                val vilkår = v.leggTilHvisIkkeEksisterer(it)
                vilkår.leggTilVurdering(
                    Vilkårsperiode(
                        periode,
                        utfall = Utfall.OPPFYLT,
                        begrunnelse = null,
                    )
                )
            }

            val tidslinje = v.rettighetstypeTidslinje()
            assertThat(tidslinje.segmenter()).hasSize(1)
            assertThat(tidslinje.segmenter().first().verdi).isEqualTo(RettighetsType.SYKEPENGEERSTATNING)
            assertThat(tidslinje.helePerioden()).isEqualTo(periode)
        }

        @Test
        fun `om bistands-vilkåret ikke er i midten får vi brudd på tidslinjen`() {
            val v = Vilkårsresultat()
            v.leggTilHvisIkkeEksisterer(Vilkårtype.BISTANDSVILKÅRET).leggTilVurdering(
                Vilkårsperiode(
                    Periode(LocalDate.now(), LocalDate.now().plusDays(10)),
                    utfall = Utfall.OPPFYLT,
                    begrunnelse = null,
                )
            )
            v.leggTilHvisIkkeEksisterer(Vilkårtype.BISTANDSVILKÅRET).leggTilVurdering(
                Vilkårsperiode(
                    Periode(LocalDate.now().plusDays(10), LocalDate.now().plusDays(20)),
                    utfall = Utfall.IKKE_OPPFYLT,
                    avslagsårsak = Avslagsårsak.MANGLENDE_DOKUMENTASJON,
                    begrunnelse = null,
                )
            )
            v.leggTilHvisIkkeEksisterer(Vilkårtype.BISTANDSVILKÅRET).leggTilVurdering(
                Vilkårsperiode(
                    Periode(LocalDate.now().plusDays(20), LocalDate.now().plusDays(30)),
                    utfall = Utfall.OPPFYLT,
                    begrunnelse = null,
                )
            )
            val tidslinje = v.rettighetstypeTidslinje()
            assertThat(tidslinje.segmenter()).hasSize(2)
            assertThat(tidslinje.erSammenhengende()).isFalse
            assertThat(tidslinje.helePerioden()).isEqualTo(Periode(LocalDate.now(), LocalDate.now().plusDays(30)))
        }

        @Test
        fun `overlapp mellom vilkår (sykepenge-erstatning først, så student)`() {
            val v = Vilkårsresultat()
            v.leggTilHvisIkkeEksisterer(Vilkårtype.SYKEPENGEERSTATNING).leggTilVurdering(
                Vilkårsperiode(
                    Periode(LocalDate.now().minusDays(5), LocalDate.now().plusDays(10)),
                    utfall = Utfall.OPPFYLT,
                    begrunnelse = null,
                )
            )
            v.leggTilHvisIkkeEksisterer(Vilkårtype.BISTANDSVILKÅRET).leggTilVurdering(
                Vilkårsperiode(
                    Periode(LocalDate.now().minusDays(5), LocalDate.now().plusDays(10)),
                    utfall = Utfall.OPPFYLT,
                    begrunnelse = null,
                )
            )
            v.leggTilHvisIkkeEksisterer(Vilkårtype.BISTANDSVILKÅRET).leggTilVurdering(
                Vilkårsperiode(
                    Periode(LocalDate.now().plusDays(1), LocalDate.now().plusDays(15)),
                    utfall = Utfall.OPPFYLT,
                    innvilgelsesårsak = Innvilgelsesårsak.STUDENT,
                    begrunnelse = null,
                )
            )

            val res = v.rettighetstypeTidslinje().komprimer()
            assertThat(res.segmenter()).hasSize(2)
            assertThat(res.segmenter().first().verdi).isEqualTo(RettighetsType.SYKEPENGEERSTATNING)
            // Kun sykepengererstatning først
            assertThat(res.segmenter().first().periode).isEqualTo(
                Periode(
                    LocalDate.now().minusDays(5),
                    LocalDate.now()
                )
            )
            // Student-hjemmel får prioritet
            assertThat(res.segmenter().toList()[1].verdi).isEqualTo(RettighetsType.STUDENT)
            assertThat(res.segmenter().toList()[1].periode).isEqualTo(
                Periode(
                    LocalDate.now().plusDays(1),
                    LocalDate.now().plusDays(15)
                )
            )
        }


        @Test
        fun `ignorerer perioder hvor bistandsvilkåret ikke er oppfylt`() {
            val v = Vilkårsresultat()
            v.leggTilHvisIkkeEksisterer(Vilkårtype.BISTANDSVILKÅRET).leggTilVurdering(
                Vilkårsperiode(
                    Periode(LocalDate.now().minusDays(5), LocalDate.now().plusDays(3)),
                    utfall = Utfall.OPPFYLT,
                    begrunnelse = null,
                )
            )
            v.leggTilHvisIkkeEksisterer(Vilkårtype.BISTANDSVILKÅRET).leggTilVurdering(
                Vilkårsperiode(
                    Periode(LocalDate.now().minusDays(2), LocalDate.now()),
                    utfall = Utfall.IKKE_OPPFYLT,
                    avslagsårsak = Avslagsårsak.MANGLENDE_DOKUMENTASJON,
                    begrunnelse = null,
                )
            )
            v.leggTilHvisIkkeEksisterer(Vilkårtype.BISTANDSVILKÅRET).leggTilVurdering(
                Vilkårsperiode(
                    Periode(LocalDate.now().plusDays(1), LocalDate.now().plusDays(15)),
                    utfall = Utfall.OPPFYLT,
                    begrunnelse = null,
                )
            )

            val res = v.rettighetstypeTidslinje().komprimer()
            assertThat(res.segmenter()).hasSize(2)
            assertThat(res.segmenter().first().verdi).isEqualTo(RettighetsType.BISTANDSBEHOV)
            // Kun sykepengererstatning først
            assertThat(res.segmenter().first().periode).isEqualTo(
                Periode(
                    LocalDate.now().minusDays(5),
                    LocalDate.now().minusDays(3)
                )
            )
            // Student-hjemmel får prioritet
            assertThat(res.segmenter().toList()[1].verdi).isEqualTo(RettighetsType.BISTANDSBEHOV)
            assertThat(res.segmenter().toList()[1].periode).isEqualTo(
                Periode(
                    LocalDate.now().plusDays(1),
                    LocalDate.now().plusDays(15)
                )
            )
        }
    }
}