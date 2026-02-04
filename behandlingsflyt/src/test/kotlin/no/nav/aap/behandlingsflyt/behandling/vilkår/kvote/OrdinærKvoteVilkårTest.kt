package no.nav.aap.behandlingsflyt.behandling.vilkår.kvote

import no.nav.aap.behandlingsflyt.behandling.rettighetstype.KvoteBruktOpp
import no.nav.aap.behandlingsflyt.behandling.rettighetstype.KvoteOk
import no.nav.aap.behandlingsflyt.behandling.rettighetstype.vurderRettighetstypeOgKvoter
import no.nav.aap.behandlingsflyt.behandling.underveis.Kvoter
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

class OrdinærKvoteVilkårTest {

    @Test
    fun `Skal opprette vilkår for når ordinær kvote er oppfylt`() {

        // Sett opp vilkårsresultat
        val rettighetsperiode = Periode(1 januar 2024, Tid.MAKS) // Mandag

        val studentOppfylt = Periode(1 januar 2024, 7 januar 2024) // Bruker 5 dager av ordinær kvote
        val speOppfylt =
            Periode(8 januar 2024, 14 januar 2024) // Bruker 5 dager av sykepengeerstatning-kvote
        val andrePeriodeBistandOppfylt = Periode(15 januar 2024, Tid.MAKS) // Bruker gjenværende dager av ordinær kvote

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
                        andrePeriodeBistandOppfylt,
                        Utfall.OPPFYLT,
                        false,
                        null,
                        faktagrunnlag = null,
                        avslagsårsak = null
                    )
                )
            ),
            sykepengeerstatningVilkåret = Vilkår(
                Vilkårtype.SYKEPENGEERSTATNING, setOf(

                    Vilkårsperiode(
                        speOppfylt,
                        Utfall.OPPFYLT,
                        false,
                        null,
                        faktagrunnlag = null,
                        avslagsårsak = null
                    )
                )
            )
        )

        // Vurder kvote (skjer i steget)

        val kvoter = Kvoter.create(ordinærkvote = 10, sykepengeerstatningkvote = 5)
        val kvotevurdering = vurderRettighetstypeOgKvoter(
            vilkårsresultat,
            kvoter
        )

        // Verifiser forventet resultat av vurderingen
        val forventetSistePeriodeOppfyltOrdinær = Periode(
            andrePeriodeBistandOppfylt.fom,
            21 januar 2024 // Siste dag med kvote
        )

        val forventetPeriodeOrdinærKvoteBruktOpp = Periode(
            22 januar 2024,
            Tid.MAKS
        )

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
            forventetSistePeriodeOppfyltOrdinær to {
                assertThat(it is KvoteOk).isTrue()
                assertThat(it.rettighetsType).isEqualTo(RettighetsType.BISTANDSBEHOV)
            },
            forventetPeriodeOrdinærKvoteBruktOpp to {
                assertThat(it is KvoteBruktOpp).isTrue()
                assertThat(it.rettighetsType).isEqualTo(RettighetsType.BISTANDSBEHOV)
            }
        )

        // Kjør vilkåret
        OrdinærKvoteVilkår(vilkårsresultat).vurder(OrdinærKvoteFaktagrunnlag(kvotevurdering, kvoter))

        val vilkåret = vilkårsresultat.finnVilkår(Vilkårtype.ORDINÆR_KVOTE)

        assertTidslinje(
            vilkåret.tidslinje(),
            studentOppfylt to { assertThat(it.utfall).isEqualTo(Utfall.OPPFYLT) },
            speOppfylt to { assertThat(it.utfall).isEqualTo(Utfall.IKKE_RELEVANT) },
            forventetSistePeriodeOppfyltOrdinær to { assertThat(it.utfall).isEqualTo(Utfall.OPPFYLT) },
            forventetPeriodeOrdinærKvoteBruktOpp to { assertThat(it.utfall).isEqualTo(Utfall.IKKE_OPPFYLT) },
        )
    }
}
