package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype.BISTANDSVILKÅRET
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype.SYKDOMSVILKÅRET
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
            val dagensDato = LocalDate.now()
            val førstePeriode = Periode(dagensDato.minusDays(5), dagensDato)
            v.leggTilHvisIkkeEksisterer(BISTANDSVILKÅRET).leggTilVurdering(
                Vilkårsperiode(
                    førstePeriode,
                    utfall = Utfall.IKKE_RELEVANT,
                    begrunnelse = null
                )
            )
            v.leggTilHvisIkkeEksisterer(SYKDOMSVILKÅRET).leggTilVurdering(
                Vilkårsperiode(
                    førstePeriode,
                    utfall = Utfall.OPPFYLT,
                    innvilgelsesårsak = Innvilgelsesårsak.SYKEPENGEERSTATNING,
                    begrunnelse = null,
                )
            )
            val andrePeriode = Periode(dagensDato.plusDays(1), dagensDato.plusDays(15))
            v.leggTilHvisIkkeEksisterer(BISTANDSVILKÅRET).leggTilVurdering(
                Vilkårsperiode(
                    andrePeriode,
                    utfall = Utfall.OPPFYLT,
                    innvilgelsesårsak = Innvilgelsesårsak.STUDENT,
                    begrunnelse = null
                )
            )
            v.leggTilHvisIkkeEksisterer(SYKDOMSVILKÅRET).leggTilVurdering(
                Vilkårsperiode(
                    andrePeriode,
                    utfall = Utfall.OPPFYLT,
                    innvilgelsesårsak = Innvilgelsesårsak.STUDENT,
                    begrunnelse = null,
                )
            )

            val res = v.rettighetstypeTidslinje()
            assertTidslinje(
                res,
                førstePeriode to {
                    assertThat(it).`as`(dagensDato.toString()).isEqualTo(RettighetsType.SYKEPENGEERSTATNING)
                },

                andrePeriode to {
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
        fun `om sykdoms-vilkåret er avslått, kan man få rett ved 11-13`() {
            val v = tomVurdering()
            val nå = LocalDate.now()

            val sykepengerPeriode = nå.plusDays(5)
            v.leggTilHvisIkkeEksisterer(SYKDOMSVILKÅRET).leggTilVurdering(
                Vilkårsperiode(
                    Periode(nå, sykepengerPeriode),
                    utfall = Utfall.OPPFYLT,
                    innvilgelsesårsak = Innvilgelsesårsak.SYKEPENGEERSTATNING,
                    begrunnelse = null
                )
            )
            v.leggTilHvisIkkeEksisterer(BISTANDSVILKÅRET).leggTilVurdering(
                Vilkårsperiode(
                    Periode(nå, sykepengerPeriode),
                    utfall = Utfall.IKKE_RELEVANT,
                    begrunnelse = null,
                )
            )
            val andrePeriode = Periode(nå.plusDays(6), nå.plusDays(30))
            v.leggTilHvisIkkeEksisterer(BISTANDSVILKÅRET).leggTilVurdering(
                Vilkårsperiode(
                    andrePeriode,
                    utfall = Utfall.OPPFYLT,
                    begrunnelse = null,
                )
            )
            v.leggTilHvisIkkeEksisterer(SYKDOMSVILKÅRET).leggTilVurdering(
                Vilkårsperiode(
                    andrePeriode,
                    utfall = Utfall.OPPFYLT,
                    begrunnelse = null
                )
            )

            val res = v.rettighetstypeTidslinje().komprimer()
            assertTidslinje(
                res,
                Periode(nå, sykepengerPeriode) to {
                    assertThat(it).isEqualTo(RettighetsType.SYKEPENGEERSTATNING)
                },

                andrePeriode to {
                    assertThat(it).isEqualTo(RettighetsType.BISTANDSBEHOV)
                }
            )
        }


        @Test
        fun `om 11-5 ikke er oppfylt, kan man likevel få innvilgelse etter 11-17`() {
            val v = tomVurdering()
            val nå = LocalDate.now()

            val sykepengerPeriode = nå.plusDays(5)
            v.leggTilHvisIkkeEksisterer(Vilkårtype.OVERGANGARBEIDVILKÅRET).leggTilVurdering(
                Vilkårsperiode(
                    Periode(nå, sykepengerPeriode),
                    utfall = Utfall.OPPFYLT,
                    begrunnelse = null,
                )
            )
            val andrePeriode = Periode(nå.plusDays(6), nå.plusDays(30))
            v.leggTilHvisIkkeEksisterer(BISTANDSVILKÅRET).leggTilVurdering(
                Vilkårsperiode(
                    andrePeriode,
                    utfall = Utfall.IKKE_RELEVANT,
                    begrunnelse = null,
                )
            )
            v.leggTilHvisIkkeEksisterer(SYKDOMSVILKÅRET).leggTilVurdering(
                Vilkårsperiode(
                    andrePeriode,
                    utfall = Utfall.OPPFYLT,
                    begrunnelse = null
                )
            )

            val res = v.rettighetstypeTidslinje().komprimer()
            assertTidslinje(
                res,
                Periode(nå, sykepengerPeriode) to {
                    assertThat(it).isEqualTo(RettighetsType.ARBEIDSSØKER)
                },

            )
        }

        @Test
        fun `om 11-5 ikke er oppfylt, kan man likevel få innvilgelse etter 11-18`() {
            val v = tomVurdering()
            val nå = LocalDate.now()

            val sykepengerPeriode = nå.plusDays(5)

            v.leggTilHvisIkkeEksisterer(BISTANDSVILKÅRET).leggTilVurdering(
                Vilkårsperiode(
                    Periode(nå, sykepengerPeriode),
                    utfall = Utfall.IKKE_RELEVANT,
                    begrunnelse = null,
                )
            )
            val andrePeriode = Periode(nå.plusDays(6), nå.plusDays(30))
            v.leggTilHvisIkkeEksisterer(Vilkårtype.OVERGANGUFØREVILKÅRET).leggTilVurdering(
                Vilkårsperiode(
                    andrePeriode,
                    utfall = Utfall.OPPFYLT,
                    begrunnelse = null,
                )
            )
            v.leggTilHvisIkkeEksisterer(SYKDOMSVILKÅRET).leggTilVurdering(
                Vilkårsperiode(
                    andrePeriode,
                    utfall = Utfall.OPPFYLT,
                    begrunnelse = null
                )
            )

            val res = v.rettighetstypeTidslinje().komprimer()
            assertTidslinje(
                res,
                Periode(nå, sykepengerPeriode) to {
                    assertThat(it).isEqualTo(RettighetsType.VURDERES_FOR_UFØRETRYGD)
                },
            )
        }
    }
}