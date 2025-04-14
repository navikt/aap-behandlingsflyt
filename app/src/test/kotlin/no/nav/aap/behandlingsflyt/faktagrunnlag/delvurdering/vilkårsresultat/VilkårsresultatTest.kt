package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype.BISTANDSVILKÅRET
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype.SYKDOMSVILKÅRET
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype.SYKEPENGEERSTATNING
import no.nav.aap.behandlingsflyt.help.assertTidslinje
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

class VilkårsresultatTest {
    @Nested
    inner class RettighetsTypeTidslinjeTest {
        @Test
        fun `tomt vilkårsresultat gir ikke en veldefinert tidslinje`() {
            val v = Vilkårsresultat()

            assertThrows<Exception> {
                v.rettighetstypeTidslinje()
            }
        }

        private fun tomVurdering(): Vilkårsresultat {
            val vilkårsresultat = Vilkårsresultat()
            for (vilkårtype in Vilkårtype.entries) {
                if (vilkårtype.obligatorisk) {
                    vilkårsresultat.leggTilHvisIkkeEksisterer(vilkårtype)
                }
            }
            return vilkårsresultat
        }

        @Test
        fun `om sykdomsvilkåret er innvilget som student, så er rettighetstype 11-5_11-14`() {
            val v = tomVurdering()
            val periode = Periode(LocalDate.now(), LocalDate.now().plusDays(10))
            v.leggTilHvisIkkeEksisterer(BISTANDSVILKÅRET).leggTilVurdering(
                Vilkårsperiode(
                    periode,
                    utfall = Utfall.OPPFYLT,
                    begrunnelse = null,
                    innvilgelsesårsak = Innvilgelsesårsak.STUDENT,
                )
            )
            Vilkårtype.entries.filter { it != BISTANDSVILKÅRET }.forEach {
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
            val v = tomVurdering()
            val periode = Periode(LocalDate.now(), LocalDate.now().plusDays(10))
            v.leggTilHvisIkkeEksisterer(Vilkårtype.SYKEPENGEERSTATNING).leggTilVurdering(
                Vilkårsperiode(
                    periode,
                    utfall = Utfall.OPPFYLT,
                    begrunnelse = null,
                )
            )
            Vilkårtype.entries
                .filter { it != Vilkårtype.SYKEPENGEERSTATNING }
                .forEach {
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
            val nå = LocalDate.now()
            val v = tomVurdering()
            v.leggTilHvisIkkeEksisterer(SYKDOMSVILKÅRET).leggTilVurdering(
                Vilkårsperiode(
                    Periode(nå, nå.plusDays(30)),
                    utfall = Utfall.OPPFYLT,
                    begrunnelse = null,
                )
            )
            v.leggTilHvisIkkeEksisterer(BISTANDSVILKÅRET).leggTilVurdering(
                Vilkårsperiode(
                    Periode(nå, nå.plusDays(10)),
                    utfall = Utfall.OPPFYLT,
                    begrunnelse = null,
                )
            )
            v.leggTilHvisIkkeEksisterer(BISTANDSVILKÅRET).leggTilVurdering(
                Vilkårsperiode(
                    Periode(nå.plusDays(10), nå.plusDays(20)),
                    utfall = Utfall.IKKE_OPPFYLT,
                    avslagsårsak = Avslagsårsak.MANGLENDE_DOKUMENTASJON,
                    begrunnelse = null,
                )
            )
            v.leggTilHvisIkkeEksisterer(BISTANDSVILKÅRET).leggTilVurdering(
                Vilkårsperiode(
                    Periode(nå.plusDays(20), nå.plusDays(30)),
                    utfall = Utfall.OPPFYLT,
                    begrunnelse = null,
                )
            )
            val tidslinje = v.rettighetstypeTidslinje()
            assertThat(tidslinje.segmenter()).hasSize(2)
            assertThat(tidslinje.erSammenhengende()).isFalse
            assertThat(tidslinje.helePerioden()).isEqualTo(Periode(nå, nå.plusDays(30)))
        }

        @Test
        fun `overlapp mellom vilkår (sykepenge-erstatning først, så student)`() {
            val v = tomVurdering()
            v.leggTilHvisIkkeEksisterer(Vilkårtype.SYKEPENGEERSTATNING).leggTilVurdering(
                Vilkårsperiode(
                    Periode(LocalDate.now().minusDays(5), LocalDate.now().plusDays(10)),
                    utfall = Utfall.OPPFYLT,
                    begrunnelse = null,
                )
            )
            v.leggTilHvisIkkeEksisterer(SYKDOMSVILKÅRET).leggTilVurdering(
                Vilkårsperiode(
                    Periode(LocalDate.now().minusDays(5), LocalDate.now().plusDays(15)),
                    utfall = Utfall.OPPFYLT,
                    begrunnelse = null
                )
            )
            v.leggTilHvisIkkeEksisterer(BISTANDSVILKÅRET).leggTilVurdering(
                Vilkårsperiode(
                    Periode(LocalDate.now().minusDays(5), LocalDate.now().plusDays(10)),
                    utfall = Utfall.OPPFYLT,
                    begrunnelse = null,
                )
            )
            v.leggTilHvisIkkeEksisterer(BISTANDSVILKÅRET).leggTilVurdering(
                Vilkårsperiode(
                    Periode(LocalDate.now().plusDays(1), LocalDate.now().plusDays(15)),
                    utfall = Utfall.OPPFYLT,
                    innvilgelsesårsak = Innvilgelsesårsak.STUDENT,
                    begrunnelse = null,
                )
            )

            val res = v.rettighetstypeTidslinje()
            assertTidslinje(
                res,
                Periode(LocalDate.now().minusDays(5), LocalDate.now()) to {
                    assertThat(it).isEqualTo(RettighetsType.SYKEPENGEERSTATNING)
                },

                Periode(LocalDate.now().plusDays(1), LocalDate.now().plusDays(15)) to {
                    assertThat(it).isEqualTo(RettighetsType.STUDENT)
                }
            )
        }


        @Test
        fun `ignorerer perioder hvor bistandsvilkåret ikke er oppfylt`() {
            val v = tomVurdering()
            val nå = LocalDate.now()
            v.leggTilHvisIkkeEksisterer(SYKDOMSVILKÅRET).leggTilVurdering(
                Vilkårsperiode(
                    Periode(nå.minusDays(5), nå.plusDays(15)),
                    utfall = Utfall.OPPFYLT,
                    begrunnelse = null
                )
            )
            v.leggTilHvisIkkeEksisterer(BISTANDSVILKÅRET).leggTilVurdering(
                Vilkårsperiode(
                    Periode(nå.minusDays(5), nå.plusDays(3)),
                    utfall = Utfall.OPPFYLT,
                    begrunnelse = null,
                )
            )
            v.leggTilHvisIkkeEksisterer(BISTANDSVILKÅRET).leggTilVurdering(
                Vilkårsperiode(
                    Periode(nå.minusDays(2), nå),
                    utfall = Utfall.IKKE_OPPFYLT,
                    avslagsårsak = Avslagsårsak.MANGLENDE_DOKUMENTASJON,
                    begrunnelse = null,
                )
            )
            v.leggTilHvisIkkeEksisterer(BISTANDSVILKÅRET).leggTilVurdering(
                Vilkårsperiode(
                    Periode(nå.plusDays(1), nå.plusDays(15)),
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
                    nå.minusDays(5),
                    nå.minusDays(3)
                )
            )
            // Student-hjemmel får prioritet
            assertThat(res.segmenter().toList()[1].verdi).isEqualTo(RettighetsType.BISTANDSBEHOV)
            assertThat(res.segmenter().toList()[1].periode).isEqualTo(
                Periode(
                    nå.plusDays(1),
                    nå.plusDays(15)
                )
            )
        }

        @Test
        fun `om sykdoms-vilkået er avslått, kan man få rett ved 11-13`() {
            val v = tomVurdering()
            val nå = LocalDate.now()
            // Bistandsvilkåret er oppfylt hele perioden (30 dager)
            v.leggTilHvisIkkeEksisterer(BISTANDSVILKÅRET).leggTilVurdering(
                Vilkårsperiode(
                    Periode(nå, nå.plusDays(30)),
                    utfall = Utfall.OPPFYLT,
                    begrunnelse = null,
                )
            )
            v.leggTilHvisIkkeEksisterer(SYKDOMSVILKÅRET).leggTilVurdering(
                Vilkårsperiode(
                    Periode(nå, nå.plusDays(5)),
                    utfall = Utfall.IKKE_OPPFYLT,
                    avslagsårsak = Avslagsårsak.IKKE_SYKDOM_AV_VISS_VARIGHET,
                    begrunnelse = null
                )
            )
            v.leggTilHvisIkkeEksisterer(SYKEPENGEERSTATNING).leggTilVurdering(
                Vilkårsperiode(
                    Periode(nå, nå.plusDays(5)),
                    utfall = Utfall.OPPFYLT,
                    begrunnelse = null,
                )
            )
            v.leggTilHvisIkkeEksisterer(SYKDOMSVILKÅRET).leggTilVurdering(
                Vilkårsperiode(
                    Periode(nå.plusDays(6), nå.plusDays(30)),
                    utfall = Utfall.OPPFYLT,
                    begrunnelse = null
                )
            )

            val res = v.rettighetstypeTidslinje().komprimer()
            assertTidslinje(
                res,
                Periode(nå, nå.plusDays(5)) to {
                    assertThat(it).isEqualTo(RettighetsType.SYKEPENGEERSTATNING)
                },

                Periode(nå.plusDays(6), nå.plusDays(30)) to {
                    assertThat(it).isEqualTo(RettighetsType.BISTANDSBEHOV)
                }
            )
        }
    }
}