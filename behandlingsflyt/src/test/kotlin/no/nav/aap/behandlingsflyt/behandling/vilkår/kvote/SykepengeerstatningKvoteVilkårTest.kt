package no.nav.aap.behandlingsflyt.behandling.vilkår.kvote

import no.nav.aap.behandlingsflyt.behandling.rettighetstype.KvoteBruktOpp
import no.nav.aap.behandlingsflyt.behandling.rettighetstype.KvoteOk
import no.nav.aap.behandlingsflyt.behandling.rettighetstype.vurderRettighetstypeOgKvoter
import no.nav.aap.behandlingsflyt.behandling.underveis.Kvoter
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Hverdager
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Hverdager.Companion.plusHverdager
import no.nav.aap.behandlingsflyt.behandling.vilkår.sykdom.SykepengerErstatningFaktagrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkår
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.help.assertTidslinje
import no.nav.aap.behandlingsflyt.help.genererVilkårsresultat
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.behandlingsflyt.test.juli
import no.nav.aap.behandlingsflyt.test.oktober
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tid
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SykepengeerstatningKvoteVilkårTest {

    @Test
    fun `Skal opprette vilkår for når sykepengeerstatning-kvote er oppfylt`() {

        // Sett opp vilkårsresultat
        val rettighetsperiode = Periode(1 januar 2024, Tid.MAKS) // Mandag

        // Setter opp kvoter med verdier som er enkle å regne på
        val kvoter = Kvoter.create(ordinærkvote = 10, sykepengeerstatningkvote = 6)

        val studentOppfylt = Periode(1 januar 2024, 7 januar 2024) // Bruker 5 dager av ordinær kvote
        val speOppfylt =
            Periode(8 januar 2024, 14 januar 2024) // Bruker 5 dager av sykepengeerstatning-kvote
        val periodeBistandOppfylt = Periode(15 januar 2024, 21 januar 2024) // Bruker gjenværende dager av ordinær kvote
        val andrePeriodeSpeOpppfylt = Periode(22 januar 2024, Tid.MAKS) // Bruker siste dag med spe-kvote

        val vilkårsresultat = genererVilkårsresultat(
            rettighetsperiode,
            studentVilkår = Vilkår(
                Vilkårtype.STUDENT,
                setOf(
                    Vilkårsperiode(
                        studentOppfylt,
                        Utfall.OPPFYLT,
                        false,
                        null,
                        faktagrunnlag = null,
                        avslagsårsak = null
                    )
                )
            ),
            bistandVilkåret = Vilkår(
                Vilkårtype.BISTANDSVILKÅRET,
                setOf(
                    Vilkårsperiode(
                        periodeBistandOppfylt,
                        Utfall.OPPFYLT,
                        false,
                        null,
                        faktagrunnlag = null,
                        avslagsårsak = null
                    )
                )
            ),
            sykepengeerstatningVilkåret = Vilkår(
                Vilkårtype.SYKEPENGEERSTATNING, listOf(speOppfylt, andrePeriodeSpeOpppfylt).map { periode ->

                    Vilkårsperiode(
                        periode,
                        Utfall.OPPFYLT,
                        false,
                        null,
                        faktagrunnlag = null,
                        avslagsårsak = null
                    )
                }.toSet()
            )
        )

        // Vurder kvote (skjer i steget)

        val kvotevurdering = vurderRettighetstypeOgKvoter(
            vilkårsresultat,
            kvoter
        )

        val forventetSistePeriodeSpeOppfylt = Periode(andrePeriodeSpeOpppfylt.fom, andrePeriodeSpeOpppfylt.fom)
        val forventetSpeKvoteBruktOpp = Periode(forventetSistePeriodeSpeOppfylt.tom.plusDays(1), Tid.MAKS)


        assertTidslinje(
            kvotevurdering,
            studentOppfylt to {
                assertThat(it is KvoteOk).isTrue()
                assertThat(it.rettighetsType).isEqualTo(RettighetsType.STUDENT)
            },
            speOppfylt to {
                assertThat(it is KvoteOk).isTrue()
                assertThat(it.rettighetsType).isEqualTo(RettighetsType.SYKEPENGEERSTATNING)
            },
            periodeBistandOppfylt to {
                assertThat(it is KvoteOk).isTrue()
                assertThat(it.rettighetsType).isEqualTo(RettighetsType.BISTANDSBEHOV)
            },
            forventetSistePeriodeSpeOppfylt to {
                assertThat(it is KvoteOk).isTrue()
                assertThat(it.rettighetsType).isEqualTo(RettighetsType.SYKEPENGEERSTATNING)
            },
            forventetSpeKvoteBruktOpp to {
                assertThat(it is KvoteBruktOpp).isTrue()
                assertThat(it.rettighetsType).isEqualTo(RettighetsType.SYKEPENGEERSTATNING)
            }
        )

        // Kjør vilkåret
        SykepengeerstatningKvoteVilkår(vilkårsresultat).vurder(
            SykepengeerstatningKvoteFaktagrunnlag(
                kvotevurdering,
                kvoter
            )
        )

        val vilkåret = vilkårsresultat.finnVilkår(Vilkårtype.SYKEPENGEERSTATNING_KVOTE)

        assertTidslinje(
            vilkåret.tidslinje(),
            studentOppfylt to { assertThat(it.utfall).isEqualTo(Utfall.IKKE_RELEVANT) },
            speOppfylt to { assertThat(it.utfall).isEqualTo(Utfall.OPPFYLT) },
            periodeBistandOppfylt to { assertThat(it.utfall).isEqualTo(Utfall.IKKE_RELEVANT) },
            forventetSistePeriodeSpeOppfylt to { assertThat(it.utfall).isEqualTo(Utfall.OPPFYLT) },
            forventetSpeKvoteBruktOpp to {
                assertThat(it.utfall).isEqualTo(Utfall.IKKE_OPPFYLT)
                assertThat(it.avslagsårsak).isEqualTo(Avslagsårsak.SYKEPENGEERSTATNINGKVOTE_BRUKT_OPP)
            },
        )
    }


    @Test
    fun `Når sykepengeerstatning er brukt opp og man får splitt på en lørdag eller søndag skal det ikke innvilges sykepengeerstatningskvote med mindre den var innvilget foregående fredag`() {

        val rettighetsperiode = Periode(24 oktober 2025, Tid.MAKS) // Mandag
        val speIkkeOppfylt = Periode(24 oktober 2025, 14 januar 2026)
        val speOppfylt = Periode(15 januar 2026, 23 oktober 2026)
        val speOppfyltSplitt = Periode(24 oktober 2026, Tid.MAKS)

        // Setter opp kvoter med verdier som er enkle å regne på
        val kvoter = Kvoter.create(ordinærkvote = 10, sykepengeerstatningkvote = 131)

        val vilkårsresultat = genererVilkårsresultat(
            rettighetsperiode,

            bistandVilkåret = Vilkår(
                Vilkårtype.BISTANDSVILKÅRET,
                setOf(
                    Vilkårsperiode(
                        rettighetsperiode,
                        Utfall.IKKE_OPPFYLT,
                        false,
                        null,
                        faktagrunnlag = null,
                        avslagsårsak = Avslagsårsak.IKKE_BEHOV_FOR_OPPFOLGING
                    )
                )
            ),
            sykepengeerstatningVilkåret = Vilkår(
                Vilkårtype.SYKEPENGEERSTATNING, setOf(
                    Vilkårsperiode(
                        speIkkeOppfylt,
                        Utfall.IKKE_OPPFYLT,
                        false,
                        "z",
                        faktagrunnlag = SykepengerErstatningFaktagrunnlag(speIkkeOppfylt, null, null),
                        avslagsårsak = Avslagsårsak.IKKE_RETT_PA_SYKEPENGEERSTATNING
                    ),
                    Vilkårsperiode(
                        speOppfylt,
                        Utfall.OPPFYLT,
                        false,
                        "x",
                        faktagrunnlag = SykepengerErstatningFaktagrunnlag(speOppfylt, null, null),
                        avslagsårsak = null
                    ),
                    Vilkårsperiode(
                        speOppfyltSplitt,
                        Utfall.OPPFYLT,
                        false,
                        "tull",
                        faktagrunnlag = SykepengerErstatningFaktagrunnlag(speOppfyltSplitt, null, null),
                        avslagsårsak = null
                    ),
                )
            )
        )
        vilkårsresultat.leggTilHvisIkkeEksisterer(Vilkårtype.SYKEPENGEERSTATNING_KVOTE).leggTilVurdering(
            Vilkårsperiode(
                periode = speIkkeOppfylt,
                vilkårsvurdering = Vilkårsvurdering(
                    utfall = Utfall.IKKE_RELEVANT,
                    manuellVurdering = false,
                    begrunnelse = null,
                    innvilgelsesårsak = null,
                    avslagsårsak = null,
                    faktagrunnlag = null
                )
            )
        )
        vilkårsresultat.leggTilHvisIkkeEksisterer(Vilkårtype.SYKEPENGEERSTATNING_KVOTE).leggTilVurdering(
            Vilkårsperiode(
                periode = Periode(24 oktober 2025, 16 juli 2026),
                vilkårsvurdering = Vilkårsvurdering(
                    utfall = Utfall.OPPFYLT,
                    manuellVurdering = false,
                    begrunnelse = null,
                    innvilgelsesårsak = null,
                    avslagsårsak = null,
                    faktagrunnlag = null
                )
            )
        )
        vilkårsresultat.leggTilHvisIkkeEksisterer(Vilkårtype.SYKEPENGEERSTATNING_KVOTE).leggTilVurdering(
            Vilkårsperiode(
                periode = Periode(17 juli 2026, 23 oktober 2026),
                vilkårsvurdering = Vilkårsvurdering(
                    utfall = Utfall.IKKE_OPPFYLT,
                    manuellVurdering = false,
                    begrunnelse = "ok",
                    innvilgelsesårsak = null,
                    avslagsårsak = Avslagsårsak.SYKEPENGEERSTATNINGKVOTE_BRUKT_OPP,
                    faktagrunnlag = null
                )
            )
        )
        vilkårsresultat.leggTilHvisIkkeEksisterer(Vilkårtype.SYKEPENGEERSTATNING_KVOTE).leggTilVurdering(
            Vilkårsperiode(
                periode = Periode(24 oktober 2026, Tid.MAKS),
                vilkårsvurdering = Vilkårsvurdering(
                    utfall = Utfall.IKKE_OPPFYLT,
                    manuellVurdering = false,
                    begrunnelse = "doh",
                    innvilgelsesårsak = null,
                    avslagsårsak = Avslagsårsak.SYKEPENGEERSTATNINGKVOTE_BRUKT_OPP,
                    faktagrunnlag = null
                )
            )
        )

        // Vurder kvote (skjer i steget)
        val kvotevurdering = vurderRettighetstypeOgKvoter(
            vilkårsresultat,
            kvoter
        )

        val oppfyltSpeMedKvotePeriode =
            Periode(speOppfylt.fom, speOppfylt.fom.plusHverdager(kvoter.sykepengeerstatningkvote.minus(Hverdager(1))))
        val splittPeriodeKvoteBruktOpp =
            Periode(oppfyltSpeMedKvotePeriode.tom.plusDays(1), speOppfyltSplitt.fom.minusDays(1))
        assertTidslinje(
            kvotevurdering,
            speIkkeOppfylt to {
                assertThat(it is KvoteOk).isTrue()
                assertThat(it.rettighetsType).isNull()
                assertThat(it.brukerAvKvoter()).isEmpty()
            },
            oppfyltSpeMedKvotePeriode to {
                assertThat(it is KvoteOk).isTrue()
                assertThat(it.rettighetsType).isEqualTo(RettighetsType.SYKEPENGEERSTATNING)
                assertThat(it.brukerAvKvoter()).isNotEmpty()
            },
            splittPeriodeKvoteBruktOpp to {
                assertThat(it is KvoteBruktOpp).isTrue()
                assertThat(it.rettighetsType).isEqualTo(RettighetsType.SYKEPENGEERSTATNING)
                assertThat(it.brukerAvKvoter()).isEmpty()
            },
            speOppfyltSplitt to {
                assertThat(it is KvoteBruktOpp).isTrue()
                assertThat(it.rettighetsType).isEqualTo(RettighetsType.SYKEPENGEERSTATNING)
                assertThat(it.brukerAvKvoter()).isEmpty()
            },
        )

        // Kjør vilkåret
        SykepengeerstatningKvoteVilkår(vilkårsresultat).vurder(
            SykepengeerstatningKvoteFaktagrunnlag(
                kvotevurdering,
                kvoter
            )
        )

        val vilkåret = vilkårsresultat.finnVilkår(Vilkårtype.SYKEPENGEERSTATNING_KVOTE)

        assertTidslinje(
            vilkåret.tidslinje(),
            speIkkeOppfylt to { assertThat(it.utfall).isEqualTo(Utfall.IKKE_RELEVANT) },
            oppfyltSpeMedKvotePeriode to { assertThat(it.utfall).isEqualTo(Utfall.OPPFYLT) },
            splittPeriodeKvoteBruktOpp to {
                assertThat(it.utfall).isEqualTo(Utfall.IKKE_OPPFYLT)
                assertThat(it.avslagsårsak).isEqualTo(Avslagsårsak.SYKEPENGEERSTATNINGKVOTE_BRUKT_OPP)
            },
            speOppfyltSplitt to {
                assertThat(it.utfall).isEqualTo(Utfall.IKKE_OPPFYLT)
                assertThat(it.avslagsårsak).isEqualTo(Avslagsårsak.SYKEPENGEERSTATNINGKVOTE_BRUKT_OPP)
            },
        )
    }


    @Test
    fun `Når sykepengeerstatning brukes opp på fredag skal man få innvilget til og med søndag`() {

        val rettighetsperiode = Periode(24 oktober 2025, Tid.MAKS) // Mandag
        val speIkkeOppfylt = Periode(24 oktober 2025, 14 januar 2026)
        val speOppfylt = Periode(15 januar 2026, 23 oktober 2026)
        val speOppfyltSplitt = Periode(24 oktober 2026, Tid.MAKS)

        val kvoter = Kvoter.create(ordinærkvote = 10, sykepengeerstatningkvote = 132)

        val vilkårsresultat = genererVilkårsresultat(
            rettighetsperiode,

            bistandVilkåret = Vilkår(
                Vilkårtype.BISTANDSVILKÅRET,
                setOf(
                    Vilkårsperiode(
                        rettighetsperiode,
                        Utfall.IKKE_OPPFYLT,
                        false,
                        null,
                        faktagrunnlag = null,
                        avslagsårsak = Avslagsårsak.IKKE_BEHOV_FOR_OPPFOLGING
                    )
                )
            ),
            sykepengeerstatningVilkåret = Vilkår(
                Vilkårtype.SYKEPENGEERSTATNING, setOf(
                    Vilkårsperiode(
                        speIkkeOppfylt,
                        Utfall.IKKE_OPPFYLT,
                        false,
                        "z",
                        faktagrunnlag = SykepengerErstatningFaktagrunnlag(speIkkeOppfylt, null, null),
                        avslagsårsak = Avslagsårsak.IKKE_RETT_PA_SYKEPENGEERSTATNING
                    ),
                    Vilkårsperiode(
                        speOppfylt,
                        Utfall.OPPFYLT,
                        false,
                        "x",
                        faktagrunnlag = SykepengerErstatningFaktagrunnlag(speOppfylt, null, null),
                        avslagsårsak = null
                    ),
                    Vilkårsperiode(
                        speOppfyltSplitt,
                        Utfall.OPPFYLT,
                        false,
                        "tull",
                        faktagrunnlag = SykepengerErstatningFaktagrunnlag(speOppfyltSplitt, null, null),
                        avslagsårsak = null
                    ),
                )
            )
        )
        vilkårsresultat.leggTilHvisIkkeEksisterer(Vilkårtype.SYKEPENGEERSTATNING_KVOTE).leggTilVurdering(
            Vilkårsperiode(
                periode = speIkkeOppfylt,
                vilkårsvurdering = Vilkårsvurdering(
                    utfall = Utfall.IKKE_RELEVANT,
                    manuellVurdering = false,
                    begrunnelse = null,
                    innvilgelsesårsak = null,
                    avslagsårsak = null,
                    faktagrunnlag = null
                )
            )
        )
        vilkårsresultat.leggTilHvisIkkeEksisterer(Vilkårtype.SYKEPENGEERSTATNING_KVOTE).leggTilVurdering(
            Vilkårsperiode(
                periode = Periode(24 oktober 2025, 18 juli 2026),
                vilkårsvurdering = Vilkårsvurdering(
                    utfall = Utfall.OPPFYLT,
                    manuellVurdering = false,
                    begrunnelse = null,
                    innvilgelsesårsak = null,
                    avslagsårsak = null,
                    faktagrunnlag = null
                )
            )
        )
        vilkårsresultat.leggTilHvisIkkeEksisterer(Vilkårtype.SYKEPENGEERSTATNING_KVOTE).leggTilVurdering(
            Vilkårsperiode(
                periode = Periode(18 juli 2026, 23 oktober 2026),
                vilkårsvurdering = Vilkårsvurdering(
                    utfall = Utfall.IKKE_OPPFYLT,
                    manuellVurdering = false,
                    begrunnelse = "ok",
                    innvilgelsesårsak = null,
                    avslagsårsak = Avslagsårsak.SYKEPENGEERSTATNINGKVOTE_BRUKT_OPP,
                    faktagrunnlag = null
                )
            )
        )
        vilkårsresultat.leggTilHvisIkkeEksisterer(Vilkårtype.SYKEPENGEERSTATNING_KVOTE).leggTilVurdering(
            Vilkårsperiode(
                periode = Periode(24 oktober 2026, Tid.MAKS),
                vilkårsvurdering = Vilkårsvurdering(
                    utfall = Utfall.IKKE_OPPFYLT,
                    manuellVurdering = false,
                    begrunnelse = "doh",
                    innvilgelsesårsak = null,
                    avslagsårsak = Avslagsårsak.SYKEPENGEERSTATNINGKVOTE_BRUKT_OPP,
                    faktagrunnlag = null
                )
            )
        )

        // Vurder kvote (skjer i steget)
        val kvotevurdering = vurderRettighetstypeOgKvoter(
            vilkårsresultat,
            kvoter
        )

        val oppfyltSpeMedKvotePeriode =
            Periode(speOppfylt.fom, 19 juli 2026)
        val splittPeriodeKvoteBruktOpp = Periode(20 juli 2026, speOppfyltSplitt.fom.minusDays(1))
        assertTidslinje(
            kvotevurdering,
            speIkkeOppfylt to {
                assertThat(it is KvoteOk).isTrue()
                assertThat(it.rettighetsType).isNull()
                assertThat(it.brukerAvKvoter()).isEmpty()
            },
            oppfyltSpeMedKvotePeriode to {
                assertThat(it is KvoteOk).isTrue()
                assertThat(it.rettighetsType).isEqualTo(RettighetsType.SYKEPENGEERSTATNING)
                assertThat(it.brukerAvKvoter()).isNotEmpty()
            },
            splittPeriodeKvoteBruktOpp to {
                assertThat(it is KvoteBruktOpp).isTrue()
                assertThat(it.rettighetsType).isEqualTo(RettighetsType.SYKEPENGEERSTATNING)
                assertThat(it.brukerAvKvoter()).isEmpty()
            },
            speOppfyltSplitt to {
                assertThat(it is KvoteBruktOpp).isTrue()
                assertThat(it.rettighetsType).isEqualTo(RettighetsType.SYKEPENGEERSTATNING)
                assertThat(it.brukerAvKvoter()).isEmpty()
            },
        )

        // Kjør vilkåret
        SykepengeerstatningKvoteVilkår(vilkårsresultat).vurder(
            SykepengeerstatningKvoteFaktagrunnlag(
                kvotevurdering,
                kvoter
            )
        )

        val vilkåret = vilkårsresultat.finnVilkår(Vilkårtype.SYKEPENGEERSTATNING_KVOTE)

        assertTidslinje(
            vilkåret.tidslinje(),
            speIkkeOppfylt to { assertThat(it.utfall).isEqualTo(Utfall.IKKE_RELEVANT) },
            oppfyltSpeMedKvotePeriode to { assertThat(it.utfall).isEqualTo(Utfall.OPPFYLT) },
            splittPeriodeKvoteBruktOpp to {
                assertThat(it.utfall).isEqualTo(Utfall.IKKE_OPPFYLT)
                assertThat(it.avslagsårsak).isEqualTo(Avslagsårsak.SYKEPENGEERSTATNINGKVOTE_BRUKT_OPP)
            },
            speOppfyltSplitt to {
                assertThat(it.utfall).isEqualTo(Utfall.IKKE_OPPFYLT)
                assertThat(it.avslagsårsak).isEqualTo(Avslagsårsak.SYKEPENGEERSTATNINGKVOTE_BRUKT_OPP)
            },
        )
    }
}
