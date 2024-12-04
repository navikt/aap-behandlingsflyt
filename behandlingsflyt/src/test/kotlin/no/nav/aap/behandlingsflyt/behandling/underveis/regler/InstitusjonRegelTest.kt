package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.behandlingsflyt.behandling.etannetsted.EtAnnetStedInput
import no.nav.aap.behandlingsflyt.behandling.etannetsted.EtAnnetStedUtlederService
import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.barnetillegg.BarnetilleggRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.Gradering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Institusjon
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.InstitusjonsoppholdRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Institusjonstype
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Oppholdstype
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.HelseinstitusjonVurdering
import no.nav.aap.behandlingsflyt.test.Fakes
import no.nav.aap.behandlingsflyt.test.MockConnection
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.komponenter.verdityper.TimerArbeid
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

@Fakes
class InstitusjonRegelTest {
    private val mockConnection = MockConnection().toDBConnection()
    val utlederService = EtAnnetStedUtlederService(
        BarnetilleggRepository(mockConnection),
        InstitusjonsoppholdRepository(mockConnection),
        SakOgBehandlingService(mockConnection)
    )

    val regel = InstitusjonRegel()

    @Test
    fun vurder() {
        val periode = Periode(LocalDate.of(2024, 1, 5), LocalDate.of(2025, 5, 1))
        val vurderingFraTidligereResultat = Vurdering(
            emptyList(),
            MeldepliktVurdering(
                null, Utfall.OPPFYLT
            ),
            null,
            null,
            null,
            Gradering(
                totaltAntallTimer = TimerArbeid(BigDecimal.ZERO),
                andelArbeid = Prosent(0),
                fastsattArbeidsevne = Prosent.`0_PROSENT`,
                gradering = Prosent.`100_PROSENT`
            ),
            grenseverdi = Prosent(60)
        ).leggTilVurdering(EnkelVurdering(Vilkårtype.ALDERSVILKÅRET, Utfall.OPPFYLT))

        val utlederInput = EtAnnetStedInput(
            institusjonsOpphold = listOf(
                Segment(
                    Periode(LocalDate.of(2024, 1, 15), (LocalDate.of(2024, 7, 15))),
                    Institusjon(
                        Institusjonstype.HS,
                        Oppholdstype.D,
                        "123",
                        "test"
                    )
                ),
                Segment(
                    Periode(LocalDate.of(2024, 7, 20), (LocalDate.of(2024, 9, 15))),
                    Institusjon(
                        Institusjonstype.HS,
                        Oppholdstype.D,
                        "123",
                        "test"
                    )
                ),
                Segment(
                    Periode(LocalDate.of(2024, 12, 14), (LocalDate.of(2025, 1, 15))),
                    Institusjon(
                        Institusjonstype.HS,
                        Oppholdstype.D,
                        "123",
                        "test"
                    )
                )
            ),
            soningsvurderinger = emptyList(),
            barnetillegg = emptyList(),
            helsevurderinger = listOf(
                HelseinstitusjonVurdering(
                    periode = Periode(LocalDate.of(2024, 1, 15), (LocalDate.of(2024, 7, 15))),
                    begrunnelse = "lagt inn med kost og losji men forsørger",
                    faarFriKostOgLosji = true,
                    forsoergerEktefelle = false,
                    harFasteUtgifter = false
                ),
                HelseinstitusjonVurdering(
                    periode = Periode(LocalDate.of(2024, 7, 20), (LocalDate.of(2024, 9, 15))),
                    begrunnelse = "lagt inn med kost og losji",
                    faarFriKostOgLosji = true,
                    forsoergerEktefelle = false,
                    harFasteUtgifter = false
                ),
                HelseinstitusjonVurdering(
                    periode = Periode(LocalDate.of(2024, 12, 14), (LocalDate.of(2025, 1, 15))),
                    begrunnelse = "lagt inn med kost og losji",
                    faarFriKostOgLosji = true,
                    forsoergerEktefelle = true,
                    harFasteUtgifter = false
                ),
            ),
            rettighetsperiode = Periode(periode.fom, periode.fom.plusYears(3))
        )

        val behovForAvklaringer = utlederService.utledBehov(utlederInput)
        val intitusjonsOppholdet = MapInstitusjonoppholdTilRegel.map(behovForAvklaringer)

        val input = tomUnderveisInput.copy(
            etAnnetSted = intitusjonsOppholdet,
        )

        val tidligereResultatTidslinje = Tidslinje(listOf(Segment(periode, vurderingFraTidligereResultat)))

        val resultat = regel.vurder(input, tidligereResultatTidslinje)

        assertEquals(7, resultat.count())

        // Blir lagt inn i løpet av januar men får ingen reduksjon før 1/2
        assertEquals(
            Periode(LocalDate.of(2024, 1, 5), LocalDate.of(2024, 1, 14)),
            resultat.segmenter().elementAt(0).periode
        )
        assertEquals(Prosent.`100_PROSENT`, resultat.segmenter().elementAt(0).verdi.gradering().gradering)
        assertEquals(null, resultat.segmenter().elementAt(0).verdi.institusjonVurdering?.årsak)

        // Innlagt i mer enn 3 måneder ingen reduksjon
        assertEquals(
            Periode(LocalDate.of(2024, 1, 15), LocalDate.of(2024, 4, 30)),
            resultat.segmenter().elementAt(1).periode
        )
        assertEquals(Prosent.`100_PROSENT`, resultat.segmenter().elementAt(1).verdi.gradering().gradering)
        assertEquals(
            Årsak.UTEN_REDUKSJON,
            resultat.segmenter().elementAt(1).verdi.institusjonVurdering?.årsak
        )
        // Innlagt fra 20/7, som er innen for 3 måneder fra forrige innleggese. reduksjon fra 1/8
        assertEquals(
            Periode(LocalDate.of(2024, 5, 1), (LocalDate.of(2024, 7, 15))),
            resultat.segmenter().elementAt(2).periode
        )
        assertEquals(Prosent.`50_PROSENT`, resultat.segmenter().elementAt(2).verdi.gradering().gradering)
        assertEquals(Årsak.KOST_OG_LOSJI, resultat.segmenter().elementAt(2).verdi.institusjonVurdering?.årsak)

        // Innlagt fra 20/7, som er innen for 3 måneder fra forrige innleggese. reduksjon fra 1/8
        assertEquals(
            Periode(LocalDate.of(2024, 7, 16), (LocalDate.of(2024, 7, 19))),
            resultat.segmenter().elementAt(3).periode
        )
        assertEquals(Prosent.`100_PROSENT`, resultat.segmenter().elementAt(3).verdi.gradering().gradering)
        assertEquals(null, resultat.segmenter().elementAt(3).verdi.institusjonVurdering?.årsak)
        assertEquals(
            Periode(LocalDate.of(2024, 7, 20), (LocalDate.of(2024, 7, 31))),
            resultat.segmenter().elementAt(4).periode
        )
        assertEquals(Prosent.`100_PROSENT`, resultat.segmenter().elementAt(4).verdi.gradering().gradering)
        assertEquals(Årsak.UTEN_REDUKSJON, resultat.segmenter().elementAt(4).verdi.institusjonVurdering?.årsak)

        // Reduksjon basert på vurdering
        assertEquals(
            Periode(LocalDate.of(2024, 8, 1), (LocalDate.of(2024, 9, 15))),
            resultat.segmenter().elementAt(5).periode
        )
        assertEquals(Prosent.`50_PROSENT`, resultat.segmenter().elementAt(5).verdi.gradering().gradering)
        assertEquals(
            Årsak.KOST_OG_LOSJI,
            resultat.segmenter().elementAt(5).verdi.institusjonVurdering?.årsak
        )

        // Ingen reduksjon da datoen ikke er passert (TODO: gjør testene relativ til dagens dato)
        assertEquals(
            Periode(LocalDate.of(2024, 9, 16), LocalDate.of(2025, 5, 1)),
            resultat.segmenter().elementAt(6).periode
        )
        assertEquals(Prosent.`100_PROSENT`, resultat.segmenter().elementAt(6).verdi.gradering().gradering)
        assertEquals(null, resultat.segmenter().elementAt(6).verdi.institusjonVurdering?.årsak)
    }
}