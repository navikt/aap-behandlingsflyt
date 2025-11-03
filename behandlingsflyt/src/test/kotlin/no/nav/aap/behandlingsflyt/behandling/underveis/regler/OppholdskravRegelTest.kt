package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.behandlingsflyt.behandling.oppholdskrav.OppholdskravGrunnlag
import no.nav.aap.behandlingsflyt.behandling.oppholdskrav.OppholdskravPeriode
import no.nav.aap.behandlingsflyt.behandling.oppholdskrav.OppholdskravVurdering
import no.nav.aap.behandlingsflyt.help.assertTidslinje
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.test.februar
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.behandlingsflyt.test.mai
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.type.Periode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
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

        val vurderinger = grunnlag.tilUnderveisTidslinje(rettighetsperiode)

        assertEquals(2, vurderinger.segmenter().count())
        
        vurderinger.assertTidslinje(
            Segment(Periode(rettighetsperiode.fom, 1 februar 2021)) {
                assertEquals(
                    it.vilkårsvurdering,
                    OppholdskravUnderveisVurdering.Vilkårsvurdering.BRUDD_OPPHOLDSKRAV_11_3_STANS
                )
            },
            Segment(Periode(25 mai 2021, rettighetsperiode.tom)) {
                assertEquals(
                    it.vilkårsvurdering,
                    OppholdskravUnderveisVurdering.Vilkårsvurdering.BRUDD_OPPHOLDSKRAV_11_3_STANS
                )
            },
        )
    }

    @Test
    fun `Har man ingen vurderte perioder på grunnlaget skal tidslinjen være tom slik at man vurderes til å ha rett som default`() {
        val grunnlag = OppholdskravGrunnlag(
            opprettet = LocalDateTime.now(),
            vurderinger = emptyList()
        )

        val rettighetsperiode = Periode(1 januar 2020, 1 januar 2022)

        val vurderinger = grunnlag.tilUnderveisTidslinje(rettighetsperiode)

        assertEquals(0, vurderinger.segmenter().count())
    }
}