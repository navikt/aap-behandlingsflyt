package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.behandlingsflyt.behandling.etannetsted.EtAnnetSted
import no.nav.aap.behandlingsflyt.behandling.etannetsted.Institusjon
import no.nav.aap.behandlingsflyt.behandling.etannetsted.Soning
import no.nav.aap.behandlingsflyt.behandling.underveis.Kvote
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.barnetillegg.BarnetilleggGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.barnetillegg.BarnetilleggPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.Gradering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.test.Fakes
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.tidslinje.Segment
import no.nav.aap.tidslinje.Tidslinje
import no.nav.aap.verdityper.Prosent
import no.nav.aap.verdityper.TimerArbeid
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.Period
import java.util.*
import kotlin.test.assertEquals

@Fakes
class InstitusjonRegelTest {

    @Test
    fun vurder() {
        val regel = InstitusjonRegel()

        val vurderingFraTidligereResultat = Vurdering(EnumMap(Vilkårtype::class.java), null, null, null, Gradering(TimerArbeid(BigDecimal(37.5)), Prosent(100), Prosent(100)))

        val innlagt = EtAnnetSted(Periode(LocalDate.of(2024, 1, 15), (LocalDate.of(2024, 7, 1))), Soning(false, false), Institusjon(true, false, false), "")
        val forsørgerNoenMensInnlagt = EtAnnetSted(Periode(LocalDate.of(2024, 7, 2), (LocalDate.of(2024, 7, 5))), Soning(false, false), Institusjon(true, true, false), "")
        val fasteKostnader = EtAnnetSted(Periode(LocalDate.of(2024, 7, 6), (LocalDate.of(2024, 7, 9))), Soning(false, false), Institusjon(true, false, true), "")
        val innlagtMedReduksjon = EtAnnetSted(Periode(LocalDate.of(2024, 7, 10), (LocalDate.of(2024, 7, 15))), Soning(false, false), Institusjon(true, false, false), "")
        val innlagtPåNytt = EtAnnetSted(Periode(LocalDate.of(2024, 7, 20), (LocalDate.of(2024, 9, 15))), Soning(false, false), Institusjon(true, false, false), "")
        val innlagtPåNyttTreMndSenere = EtAnnetSted(Periode(LocalDate.of(2024, 12, 25), (LocalDate.of(2025, 1, 15))), Soning(false, false), Institusjon(true, false, false), "")

        val intitusjonsOppholdet = listOf(innlagt, forsørgerNoenMensInnlagt, innlagtMedReduksjon, fasteKostnader, innlagtPåNytt, innlagtPåNyttTreMndSenere)
        val barnetillegg = BarnetilleggGrunnlag(1, listOf(BarnetilleggPeriode(Periode(LocalDate.of(2024, 5, 1), LocalDate.of(2024, 6, 10)), setOf())))
        val input = UnderveisInput(
            Periode(LocalDate.now(), LocalDate.now()), listOf(), listOf(), listOf(), mapOf(), null, Kvote(Period.ofDays(1)), setOf(), intitusjonsOppholdet, barnetillegg
        )

        val tidligereResultatTidslinje = Tidslinje(listOf( Segment(Periode(LocalDate.of(2024, 1, 5), LocalDate.of(2025, 5, 1)), vurderingFraTidligereResultat)))

        val resultat = regel.vurder(input, tidligereResultatTidslinje)

        assertEquals(13, resultat.count())

        //Ikke innlagt
        assertEquals(Periode(LocalDate.of(2024, 1, 5), LocalDate.of(2024, 1, 14)), resultat.segmenter().elementAt(0).periode)
        assertEquals(Prosent.`100_PROSENT`, resultat.segmenter().elementAt(0).verdi.gradering()?.gradering)
        assertEquals(null, resultat.segmenter().elementAt(0).verdi.institusjonVurdering?.årsak)

        //Friperiode 3mnd
        assertEquals(Periode(LocalDate.of(2024, 1, 15), LocalDate.of(2024, 4, 30)), resultat.segmenter().elementAt(1).periode)
        assertEquals(Prosent.`100_PROSENT`, resultat.segmenter().elementAt(1).verdi.gradering()?.gradering)
        assertEquals(Årsak.UTEN_REDUKSJON_TRE_MND, resultat.segmenter().elementAt(1).verdi.institusjonVurdering?.årsak)

        //Barneperiode
        assertEquals(Periode(LocalDate.of(2024, 5, 1), LocalDate.of(2024, 6, 10)), resultat.segmenter().elementAt(2).periode)
        assertEquals(Prosent.`100_PROSENT`, resultat.segmenter().elementAt(2).verdi.gradering()?.gradering)
        assertEquals(Årsak.BARNETILLEGG, resultat.segmenter().elementAt(2).verdi.institusjonVurdering?.årsak)

        //Reduksjon
        assertEquals(Periode(LocalDate.of(2024, 6, 11), LocalDate.of(2024, 7, 1)), resultat.segmenter().elementAt(3).periode)
        assertEquals(Prosent.`50_PROSENT`, resultat.segmenter().elementAt(3).verdi.gradering()?.gradering)
        assertEquals(Årsak.KOST_OG_LOSJI, resultat.segmenter().elementAt(3).verdi.institusjonVurdering?.årsak)

        //Forsørger noen, utover 3mnd
        assertEquals(Periode(LocalDate.of(2024, 7, 2), (LocalDate.of(2024, 7, 5))), resultat.segmenter().elementAt(4).periode)
        assertEquals(Prosent.`100_PROSENT`, resultat.segmenter().elementAt(4).verdi.gradering()?.gradering)
        assertEquals(Årsak.FORSØRGER, resultat.segmenter().elementAt(4).verdi.institusjonVurdering?.årsak)

        //Har faste kostnader
        assertEquals(Periode(LocalDate.of(2024, 7, 6), (LocalDate.of(2024, 7, 9))), resultat.segmenter().elementAt(5).periode)
        assertEquals(Prosent.`100_PROSENT`, resultat.segmenter().elementAt(5).verdi.gradering()?.gradering)
        assertEquals(Årsak.FASTE_KOSTNADER, resultat.segmenter().elementAt(5).verdi.institusjonVurdering?.årsak)

        //Reduksjon
        assertEquals(Periode(LocalDate.of(2024, 7, 10), (LocalDate.of(2024, 7, 15))), resultat.segmenter().elementAt(6).periode)
        assertEquals(Prosent.`50_PROSENT`, resultat.segmenter().elementAt(6).verdi.gradering()?.gradering)
        assertEquals(Årsak.KOST_OG_LOSJI, resultat.segmenter().elementAt(6).verdi.institusjonVurdering?.årsak)

        //Ikke innlagt
        assertEquals(Periode(LocalDate.of(2024, 7, 16),  LocalDate.of(2024, 7, 19)), resultat.segmenter().elementAt(7).periode)
        assertEquals(Prosent.`100_PROSENT`, resultat.segmenter().elementAt(7).verdi.gradering()?.gradering)
        assertEquals(null, resultat.segmenter().elementAt(7).verdi.institusjonVurdering?.årsak)

        //Friperiode resterende mnd
        assertEquals(Periode(LocalDate.of(2024, 7, 20), LocalDate.of(2024, 7, 31)), resultat.segmenter().elementAt(8).periode)
        assertEquals(Prosent.`100_PROSENT`, resultat.segmenter().elementAt(8).verdi.gradering()?.gradering)
        assertEquals(Årsak.UTEN_REDUKSJON_RESTERENDE_MND, resultat.segmenter().elementAt(8).verdi.institusjonVurdering?.årsak)

        //Reduksjon
        assertEquals(Periode(LocalDate.of(2024, 8, 1), (LocalDate.of(2024, 9, 15))), resultat.segmenter().elementAt(9).periode)
        assertEquals(Prosent.`50_PROSENT`, resultat.segmenter().elementAt(9).verdi.gradering()?.gradering)
        assertEquals(Årsak.KOST_OG_LOSJI, resultat.segmenter().elementAt(9).verdi.institusjonVurdering?.årsak)

        //Ikke innlagt
        assertEquals(Periode(LocalDate.of(2024, 9, 16), LocalDate.of(2024, 12, 24)), resultat.segmenter().elementAt(10).periode)
        assertEquals(Prosent.`100_PROSENT`, resultat.segmenter().elementAt(10).verdi.gradering()?.gradering)
        assertEquals(null, resultat.segmenter().elementAt(10).verdi.institusjonVurdering?.årsak)

        //Friperiode 3mnd
        assertEquals(Periode(LocalDate.of(2024, 12, 25), LocalDate.of(2025, 1, 15)), resultat.segmenter().elementAt(11).periode)
        assertEquals(Prosent.`100_PROSENT`, resultat.segmenter().elementAt(11).verdi.gradering()?.gradering)
        assertEquals(Årsak.UTEN_REDUKSJON_TRE_MND, resultat.segmenter().elementAt(11).verdi.institusjonVurdering?.årsak)

        //Ikke innlagt
        assertEquals(Periode(LocalDate.of(2025, 1, 16), LocalDate.of(2025, 5, 1)), resultat.segmenter().elementAt(12).periode)
        assertEquals(Prosent.`100_PROSENT`, resultat.segmenter().elementAt(12).verdi.gradering()?.gradering)
        assertEquals(null, resultat.segmenter().elementAt(12).verdi.institusjonVurdering?.årsak)
    }
}