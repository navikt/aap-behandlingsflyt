package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.behandlingsflyt.behandling.oppholdskrav.OppholdskravGrunnlag
import no.nav.aap.behandlingsflyt.behandling.oppholdskrav.OppholdskravPeriode
import no.nav.aap.behandlingsflyt.behandling.oppholdskrav.OppholdskravVurdering
import no.nav.aap.behandlingsflyt.behandling.vilkår.oppholdskrav.Oppholdskravvilkår
import no.nav.aap.behandlingsflyt.behandling.vilkår.oppholdskrav.OppholdskravvilkårGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.help.assertTidslinje
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.test.februar
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.behandlingsflyt.test.mai
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tid
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class OppholdskravRegelTest {

    @Test
    fun `Vilkårstidslinje for brudd på § 11-3 skal ignorere oppfylte perioder og begrenses til rettighetsperioden`() {
        val grunnlag = OppholdskravGrunnlag(
            opprettet = LocalDateTime.now(),
            vurderinger = listOf(
                OppholdskravVurdering(
                    opprettet = LocalDateTime.now(),
                    vurdertAv = "123",
                    vurdertIBehandling = BehandlingId(1L),
                    perioder = listOf(
                        OppholdskravPeriode(
                            fom = 1 januar 2010,
                            tom = 1 februar 2021,
                            oppfylt = false,
                            land = "Behamas",
                            begrunnelse = "en grunn"
                        ),
                        OppholdskravPeriode(
                            fom = 2 februar 2021,
                            tom = 24 mai 2021,
                            oppfylt = true,
                            land = null,
                            begrunnelse = "en grunn"
                        ),
                        OppholdskravPeriode(
                            fom = 25 mai 2021,
                            tom = null,
                            oppfylt = false,
                            land = "Behamas",
                            begrunnelse = "en grunn til"
                        )
                    )
                )
            )
        )

        val rettighetsperiode = Periode(1 januar 2020, 1 januar 2022)

        val vurderinger = vurder(grunnlag, rettighetsperiode.fom)

        assertEquals(3, vurderinger.segmenter().count())

        assertTidslinje(
            vurderinger,
            Periode(rettighetsperiode.fom, 1 februar 2021) to {
                assertEquals(Avslagsårsak.BRUDD_PÅ_OPPHOLDSKRAV_STANS, it.avslagsårsak)
            },
            Periode(2 februar 2021, 24 mai 2021) to {
                assertEquals(null, it.avslagsårsak)
            },
            Periode(25 mai 2021, Tid.MAKS) to {
                assertEquals(Avslagsårsak.BRUDD_PÅ_OPPHOLDSKRAV_STANS, it.avslagsårsak)
            },
        )
    }

    @Test
    fun `Har man ingen vurderte perioder på grunnlaget skal tidslinjen være tom slik at man vurderes til å ha rett som default`() {
        val grunnlag = OppholdskravGrunnlag(
            opprettet = LocalDateTime.now(),
            vurderinger = emptyList()
        )

        val vurderinger = vurder(grunnlag, 1 januar 2020)

        assertTidslinje(vurderinger,
            Periode(1 januar 2020, Tid.MAKS) to {
                assertThat(it.utfall).isEqualTo(Utfall.OPPFYLT)
                assertThat(it.manuellVurdering).isEqualTo(false)
            }
        )
    }

    private fun vurder(grunnlag: OppholdskravGrunnlag, vurderFra: LocalDate): Tidslinje<Vilkårsvurdering> {
        val vilkårsresultat = Vilkårsresultat()
        Oppholdskravvilkår(vilkårsresultat).vurder(OppholdskravvilkårGrunnlag(
            oppholdskravGrunnlag = grunnlag,
            vurderFra = vurderFra
        ))
        return vilkårsresultat.finnVilkår(Vilkårtype.OPPHOLDSKRAV).tidslinje()
    }
}