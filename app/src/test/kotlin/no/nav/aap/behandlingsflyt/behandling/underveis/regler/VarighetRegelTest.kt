package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.behandlingsflyt.behandling.underveis.Kvote
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.Month
import java.util.*

class VarighetRegelTest {
    private val regel = VarighetRegel()

    @Test
    fun `rett alle dager, innenfor en uke, men kvoten blir brukt opp`() {
        val rettighetsperiode = Periode(LocalDate.of(2024, Month.NOVEMBER, 18), LocalDate.of(2024, Month.NOVEMBER, 22))
        val vurdering = regel.vurder(
            tomUnderveisInput.copy(
                rettighetsperiode = rettighetsperiode,
                kvote = Kvote(4)
            ),

            Tidslinje(
                rettighetsperiode,
                Vurdering(
                    vurderinger = EnumMap(mapOf(Vilkårtype.ALDERSVILKÅRET to Utfall.OPPFYLT)),
                )
            )
        )

        assertTrue(
            vurdering.segment(
                LocalDate.of(2024, Month.NOVEMBER, 18)
            )!!.verdi.harRett()
        )
        assertFalse(
            vurdering.segment(LocalDate.of(2024, Month.NOVEMBER, 22))!!.verdi.harRett()
        )
    }
}