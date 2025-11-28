package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype.BISTANDSVILKÅRET
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype.SYKDOMSVILKÅRET
import no.nav.aap.behandlingsflyt.help.assertTidslinje
import no.nav.aap.komponenter.tidslinje.tidslinjeOf
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

class VilkårsresultatTest {
    @Nested
    inner class RettighetsTypeTidslinjeTest {
        private fun tomVurdering(): Vilkårsresultat {
            val vilkårsresultat = Vilkårsresultat()
            for (vilkårtype in Vilkårtype.entries) {
                if (vilkårtype.obligatorisk) {
                    vilkårsresultat.leggTilHvisIkkeEksisterer(vilkårtype)
                }
            }
            return vilkårsresultat
        }

        fun Vilkårsresultat.leggTilFellesVilkår(periode: Periode) {
            for (vilkår in listOf(
                Vilkårtype.ALDERSVILKÅRET,
                Vilkårtype.LOVVALG,
                Vilkårtype.GRUNNLAGET,
                Vilkårtype.MEDLEMSKAP
            )) {
                leggTilHvisIkkeEksisterer(vilkår).leggTilVurdering(
                    Vilkårsperiode(
                        periode = periode,
                        utfall = Utfall.OPPFYLT,
                        begrunnelse = null,
                        innvilgelsesårsak = null,
                    )
                )
            }
        }

        @Test
        fun `om sykdomsvilkåret er innvilget som student, så er rettighetstype 11-5_11-14`() {
            val v = tomVurdering()
            val periode = Periode(LocalDate.now(), LocalDate.now().plusDays(10))
            Vilkårtype.entries.forEach {
                val vilkår = v.leggTilHvisIkkeEksisterer(it)
                vilkår.leggTilVurdering(
                    Vilkårsperiode(
                        periode,
                        utfall = Utfall.OPPFYLT,
                        begrunnelse = null,
                        innvilgelsesårsak = if (it in listOf(SYKDOMSVILKÅRET)) Innvilgelsesårsak.STUDENT else null,
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
            v.leggTilFellesVilkår(Periode(nå, nå.plusDays(30)))
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
                    førstePeriode, utfall = Utfall.IKKE_RELEVANT, begrunnelse = null
                )
            )
            v.leggTilHvisIkkeEksisterer(Vilkårtype.SYKEPENGEERSTATNING).leggTilVurdering(
                Vilkårsperiode(
                    førstePeriode,
                    utfall = Utfall.OPPFYLT,
                    innvilgelsesårsak = null,
                    begrunnelse = null,
                )
            )
            val andrePeriode = Periode(dagensDato.plusDays(1), dagensDato.plusDays(15))
            v.leggTilHvisIkkeEksisterer(SYKDOMSVILKÅRET).leggTilVurdering(
                Vilkårsperiode(
                    andrePeriode,
                    utfall = Utfall.OPPFYLT,
                    innvilgelsesårsak = Innvilgelsesårsak.STUDENT,
                    begrunnelse = null,
                )
            )
            v.leggTilFellesVilkår(Periode(dagensDato.minusDays(5), dagensDato.plusDays(15)))

            val res = v.rettighetstypeTidslinje()
            assertTidslinje(
                res, førstePeriode to {
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
                    Periode(nå.minusDays(5), nå.plusDays(15)), utfall = Utfall.OPPFYLT, begrunnelse = null
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
            v.leggTilFellesVilkår(Periode(nå.minusDays(5), nå.plusDays(15)))

            val res = v.rettighetstypeTidslinje().komprimer()
            assertThat(res.segmenter()).hasSize(2)
            assertThat(res.segmenter().first().verdi).isEqualTo(RettighetsType.BISTANDSBEHOV)
            // Kun sykepengererstatning først
            assertThat(res.segmenter().first().periode).isEqualTo(
                Periode(
                    nå.minusDays(5), nå.minusDays(3)
                )
            )
            // Student-hjemmel får prioritet
            assertThat(res.segmenter().toList()[1].verdi).isEqualTo(RettighetsType.BISTANDSBEHOV)
            assertThat(res.segmenter().toList()[1].periode).isEqualTo(
                Periode(
                    nå.plusDays(1), nå.plusDays(15)
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
                    utfall = Utfall.IKKE_OPPFYLT,
                    avslagsårsak = Avslagsårsak.MANGLENDE_DOKUMENTASJON,
                    begrunnelse = null
                )
            )
            v.leggTilHvisIkkeEksisterer(Vilkårtype.SYKEPENGEERSTATNING).leggTilVurdering(
                Vilkårsperiode(
                    Periode(nå, sykepengerPeriode),
                    utfall = Utfall.OPPFYLT,
                    innvilgelsesårsak = null,
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
                    andrePeriode, utfall = Utfall.OPPFYLT, begrunnelse = null
                )
            )
            v.leggTilFellesVilkår(Periode(nå, nå.plusDays(30)))

            val res = v.rettighetstypeTidslinje().komprimer()
            assertTidslinje(
                res, Periode(nå, sykepengerPeriode) to {
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
            val ordinærPeriode = Periode(nå, nå.plusMonths(1).minusDays(1))
            val arbeidssøkerPeriode = Periode(ordinærPeriode.tom.plusDays(1), nå.plusYears(1).minusDays(1))

            v.leggTilHvisIkkeEksisterer(SYKDOMSVILKÅRET).leggTilVurderinger(
                tidslinjeOf(
                    ordinærPeriode to Vilkårsvurdering(
                        utfall = Utfall.OPPFYLT,
                        manuellVurdering = true,
                        begrunnelse = "",
                        faktagrunnlag = null,
                    ),
                    arbeidssøkerPeriode to Vilkårsvurdering(
                        utfall = Utfall.IKKE_OPPFYLT,
                        avslagsårsak = Avslagsårsak.IKKE_SYKDOM_SKADE_LYTE_VESENTLIGDEL,
                        manuellVurdering = true,
                        begrunnelse = "",
                        faktagrunnlag = null,
                    )
                )
            )

            v.leggTilHvisIkkeEksisterer(BISTANDSVILKÅRET).leggTilVurderinger(
                tidslinjeOf(
                    ordinærPeriode to Vilkårsvurdering(
                        utfall = Utfall.OPPFYLT,
                        manuellVurdering = true,
                        begrunnelse = "",
                        faktagrunnlag = null,
                    ),
                    arbeidssøkerPeriode to Vilkårsvurdering(
                        utfall = Utfall.IKKE_OPPFYLT,
                        avslagsårsak = Avslagsårsak.IKKE_BEHOV_FOR_OPPFOLGING,
                        manuellVurdering = true,
                        begrunnelse = "",
                        faktagrunnlag = null,
                    )
                )
            )
            v.leggTilHvisIkkeEksisterer(Vilkårtype.OVERGANGARBEIDVILKÅRET).leggTilVurderinger(
                tidslinjeOf(
                    arbeidssøkerPeriode to Vilkårsvurdering(
                        utfall = Utfall.OPPFYLT,
                        manuellVurdering = true,
                        begrunnelse = "",
                        faktagrunnlag = null,
                    ),
                )
            )
            v.leggTilFellesVilkår(ordinærPeriode)
            v.leggTilFellesVilkår(arbeidssøkerPeriode)

            val res = v.rettighetstypeTidslinje().komprimer()
            assertTidslinje(
                res,
                ordinærPeriode to {
                    assertThat(it).isEqualTo(RettighetsType.BISTANDSBEHOV)
                },
                arbeidssøkerPeriode to {
                    assertThat(it).isEqualTo(RettighetsType.ARBEIDSSØKER)
                },
            )
        }

        @Test
        fun `om 11-5 ikke er oppfylt, kan man likevel få innvilgelse etter 11-18`() {
            val v = tomVurdering()
            val nå = LocalDate.now()

            val sykepengerPeriode = nå.plusDays(30)
            v.leggTilHvisIkkeEksisterer(Vilkårtype.OVERGANGUFØREVILKÅRET).leggTilVurdering(
                Vilkårsperiode(
                    Periode(nå, sykepengerPeriode),
                    utfall = Utfall.OPPFYLT,
                    begrunnelse = null,
                    innvilgelsesårsak = null,
                )
            )

            v.leggTilHvisIkkeEksisterer(BISTANDSVILKÅRET).leggTilVurdering(
                Vilkårsperiode(
                    Periode(nå, sykepengerPeriode),
                    utfall = Utfall.IKKE_RELEVANT,
                    begrunnelse = null,
                )
            )
            v.leggTilHvisIkkeEksisterer(SYKDOMSVILKÅRET).leggTilVurdering(
                Vilkårsperiode(
                    Periode(nå, sykepengerPeriode),
                    utfall = Utfall.OPPFYLT,
                    begrunnelse = null,
                )
            )
            v.leggTilFellesVilkår(Periode(nå, sykepengerPeriode))

            val res = v.rettighetstypeTidslinje().komprimer()
            assertTidslinje(
                res,
                Periode(nå, sykepengerPeriode) to {
                    assertThat(it).isEqualTo(RettighetsType.VURDERES_FOR_UFØRETRYGD)
                },
            )
        }


        @Test
        fun `om 11-5 ikke er oppfylt, men både 11-18 og 11-13 er oppfylt skal vi innvilge basert på 11-18 (inntil videre)`() {
            val v = tomVurdering()
            val nå = LocalDate.now()

            val sykepengerPeriode = nå.plusDays(30)
            v.leggTilHvisIkkeEksisterer(Vilkårtype.OVERGANGUFØREVILKÅRET).leggTilVurdering(
                Vilkårsperiode(
                    Periode(nå, sykepengerPeriode),
                    utfall = Utfall.OPPFYLT,
                    begrunnelse = null,
                    innvilgelsesårsak = null,
                )
            )

            v.leggTilHvisIkkeEksisterer(BISTANDSVILKÅRET).leggTilVurdering(
                Vilkårsperiode(
                    Periode(nå, sykepengerPeriode),
                    utfall = Utfall.IKKE_RELEVANT,
                    begrunnelse = null,
                )
            )
            v.leggTilHvisIkkeEksisterer(SYKDOMSVILKÅRET).leggTilVurdering(
                Vilkårsperiode(
                    Periode(nå, sykepengerPeriode),
                    utfall = Utfall.IKKE_OPPFYLT,
                    avslagsårsak = Avslagsårsak.MANGLENDE_DOKUMENTASJON,
                    begrunnelse = null,
                )
            )
            v.leggTilHvisIkkeEksisterer(Vilkårtype.SYKEPENGEERSTATNING).leggTilVurdering(
                Vilkårsperiode(
                    Periode(nå, sykepengerPeriode),
                    innvilgelsesårsak = null,
                    utfall = Utfall.OPPFYLT,
                    begrunnelse = null,
                )
            )
            v.leggTilFellesVilkår(Periode(nå, sykepengerPeriode))

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