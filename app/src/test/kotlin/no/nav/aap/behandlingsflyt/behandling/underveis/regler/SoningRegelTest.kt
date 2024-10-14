package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.behandlingsflyt.behandling.etannetsted.EtAnnetSted
import no.nav.aap.behandlingsflyt.behandling.etannetsted.Soning
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.Gradering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.test.Fakes
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.tidslinje.Segment
import no.nav.aap.tidslinje.Tidslinje
import no.nav.aap.verdityper.Prosent
import no.nav.aap.verdityper.TimerArbeid
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

@Fakes
class SoningRegelTest {

    @Test
    fun vurder() {
        val regel = SoningRegel()
        val vurderingFraTidligereResultat = Vurdering(EnumMap(Vilkårtype::class.java), null, null, null, null, Gradering(TimerArbeid(BigDecimal(37.5)), Prosent(100), Prosent(100)), null, null )

        val formueUnderForvaltning = EtAnnetSted(Periode(LocalDate.of(2024, 1, 6), (LocalDate.of(2024, 1, 10))), soning = Soning(true, true, false, false), begrunnelse = "")
        val sonerIFengsel = EtAnnetSted(Periode(LocalDate.of(2024, 1, 11), (LocalDate.of(2024, 1, 15))), soning = Soning(true, false, false, false), begrunnelse = "")
        val arbeidUtenforAnstalt = EtAnnetSted(Periode(LocalDate.of(2024, 1, 16), (LocalDate.of(2024, 2, 5))), soning = Soning(true, false, false, true), begrunnelse = "")
        val sonerUtenforFengsel = EtAnnetSted(Periode(LocalDate.of(2024, 2, 6), (LocalDate.of(2024, 2, 15))), soning = Soning(true, false, true, false), begrunnelse = "")

        val soningOppholdet = listOf(formueUnderForvaltning, sonerIFengsel, arbeidUtenforAnstalt, sonerUtenforFengsel)
        val tidligereResultatTidslinje = Tidslinje(listOf( Segment(Periode(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 1)), vurderingFraTidligereResultat)))

        val input = tomUnderveisInput.copy(
            etAnnetSted = soningOppholdet,
        )

        val resultat = regel.vurder(input, tidligereResultatTidslinje)

        assertEquals(6, resultat.count())

        //Soner ikke
        assertEquals(Periode(LocalDate.of(2024, 1, 1), (LocalDate.of(2024, 1, 5))), resultat.segmenter().elementAt(0).periode)
        assertEquals(Prosent.`100_PROSENT`, resultat.segmenter().elementAt(0).verdi.gradering()?.gradering)
        assertEquals(null, resultat.segmenter().elementAt(0).verdi.institusjonVurdering?.årsak)

        //Formue under forvaltning
        assertEquals(Periode(LocalDate.of(2024, 1, 6), (LocalDate.of(2024, 1, 10))), resultat.segmenter().elementAt(1).periode)
        assertEquals(Prosent.`0_PROSENT`, resultat.segmenter().elementAt(1).verdi.gradering()?.gradering)
        assertEquals(Årsak.FORMUE_UNDER_FORVALTNING, resultat.segmenter().elementAt(1).verdi.institusjonVurdering?.årsak)

        //Soner i fengsel
        assertEquals(Periode(LocalDate.of(2024, 1, 11), (LocalDate.of(2024, 1, 15))), resultat.segmenter().elementAt(2).periode)
        assertEquals(Prosent.`0_PROSENT`, resultat.segmenter().elementAt(2).verdi.gradering()?.gradering)
        assertEquals(Årsak.SONER_I_FENGSEL, resultat.segmenter().elementAt(2).verdi.institusjonVurdering?.årsak)

        //Arbeid utenfor anstalt
        assertEquals(Periode(LocalDate.of(2024, 1, 16), (LocalDate.of(2024, 2, 5))), resultat.segmenter().elementAt(3).periode)
        assertEquals(Prosent.`100_PROSENT`, resultat.segmenter().elementAt(3).verdi.gradering()?.gradering)
        assertEquals(Årsak.ARBEID_UTENFOR_ANSTALT, resultat.segmenter().elementAt(3).verdi.institusjonVurdering?.årsak)

        //Soner utenfor fengsel
        assertEquals(Periode(LocalDate.of(2024, 2, 6), (LocalDate.of(2024, 2, 15))), resultat.segmenter().elementAt(4).periode)
        assertEquals(Prosent.`100_PROSENT`, resultat.segmenter().elementAt(4).verdi.gradering()?.gradering)
        assertEquals(Årsak.SONER_UTENFOR_FENGSEL, resultat.segmenter().elementAt(4).verdi.institusjonVurdering?.årsak)

        //Soner ikke
        assertEquals(Periode(LocalDate.of(2024, 2, 16), (LocalDate.of(2024, 12, 1))), resultat.segmenter().elementAt(5).periode)
        assertEquals(Prosent.`100_PROSENT`, resultat.segmenter().elementAt(5).verdi.gradering()?.gradering)
        assertEquals(null, resultat.segmenter().elementAt(5).verdi.institusjonVurdering?.årsak)
    }
}