package no.nav.aap.behandlingsflyt.behandling.vilkår.kvote

import no.nav.aap.behandlingsflyt.behandling.rettighetstype.KvoteBruktOpp
import no.nav.aap.behandlingsflyt.behandling.rettighetstype.KvoteOk
import no.nav.aap.behandlingsflyt.behandling.rettighetstype.vurderRettighetstypeOgKvoter
import no.nav.aap.behandlingsflyt.behandling.underveis.Kvoter
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkår
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.help.assertTidslinje
import no.nav.aap.behandlingsflyt.help.genererVilkårsresultat
import no.nav.aap.behandlingsflyt.test.januar
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
        SykepengeerstatningKvoteVilkår(vilkårsresultat).vurder(SykepengeerstatningKvoteFaktagrunnlag(kvotevurdering, kvoter))

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
}
