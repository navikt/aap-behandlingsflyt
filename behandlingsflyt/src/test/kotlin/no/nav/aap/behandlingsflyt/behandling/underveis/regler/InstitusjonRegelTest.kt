package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.behandlingsflyt.behandling.institusjonsopphold.Institusjon
import no.nav.aap.behandlingsflyt.behandling.institusjonsopphold.Institusjonsopphold
import no.nav.aap.behandlingsflyt.behandling.institusjonsopphold.InstitusjonsoppholdInput
import no.nav.aap.behandlingsflyt.behandling.institusjonsopphold.InstitusjonsoppholdUtlederService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.ArbeidsGradering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Institusjonstype
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Oppholdstype
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.HelseinstitusjonVurdering
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.barnetillegg.BarnetilleggRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.register.institusjonsopphold.InstitusjonsoppholdRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
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
    val utlederService = InstitusjonsoppholdUtlederService(
        BarnetilleggRepositoryImpl(mockConnection),
        InstitusjonsoppholdRepositoryImpl(mockConnection),
        SakRepositoryImpl(mockConnection),
        BehandlingRepositoryImpl(mockConnection)
    )

    val regel = InstitusjonRegel()

    // Alle datoer i testen er forankret relativt til i dag slik at testen alltid er gyldig.
    // Oppholdet legges 2 år tilbake i tid, slik at alle perioder alltid er i fortiden.
    private val idag = LocalDate.now()
    private val innleggelseStart = idag.minusYears(2).withDayOfMonth(15)

    // Reduksjon starter 4 måneder etter første dag i innleggelsesmåneden (standard, ikke umiddelbar)
    private val reduksjonStart = innleggelseStart.withDayOfMonth(1).plusMonths(4)
    private val innleggelseSlutt = innleggelseStart.plusMonths(6)
    private val rettighetsperiodeFom = innleggelseStart.minusMonths(1)
    private val rettighetsperiodeTom = innleggelseStart.plusYears(3)

    private fun lagVurdering() = Vurdering(
        fårAapEtter = RettighetsType.BISTANDSBEHOV,
        meldepliktVurdering = MeldepliktVurdering.Fritak,
        gradering = ArbeidsGradering(
            totaltAntallTimer = TimerArbeid(BigDecimal.ZERO),
            andelArbeid = Prosent(0),
            fastsattArbeidsevne = Prosent.`0_PROSENT`,
            gradering = Prosent.`100_PROSENT`,
            opplysningerMottatt = null,
        ),
        grenseverdi = Prosent(60)
    )

    @Test
    fun `ingen institusjonsopphold gir uendret resultat`() {
        val periode = Periode(rettighetsperiodeFom, rettighetsperiodeTom)
        val input = tomUnderveisInput.copy(institusjonsopphold = emptyList())
        val tidligereResultat = Tidslinje(listOf(Segment(periode, lagVurdering())))

        val resultat = regel.vurder(input, tidligereResultat)

        assertEquals(1, resultat.segmenter().count())
        assertEquals(periode, resultat.segmenter().first().periode)
        assertEquals(null, resultat.segmenter().first().verdi.institusjonVurdering)
    }

    @Test
    fun `opphold der erPåInstitusjon er false filtreres bort og gir uendret resultat`() {
        val periode = Periode(rettighetsperiodeFom, rettighetsperiodeTom)
        val input = tomUnderveisInput.copy(
            institusjonsopphold = listOf(
                Institusjonsopphold(
                    periode = Periode(innleggelseStart, innleggelseSlutt),
                    institusjon = Institusjon(
                        erPåInstitusjon = false,
                        skalGiReduksjon = true,
                        skalGiUmiddelbarReduksjon = false
                    )
                )
            )
        )
        val tidligereResultat = Tidslinje(listOf(Segment(periode, lagVurdering())))

        val resultat = regel.vurder(input, tidligereResultat)

        assertEquals(1, resultat.segmenter().count())
        assertEquals(null, resultat.segmenter().first().verdi.institusjonVurdering)
    }

    @Test
    fun `opphold med forsørger eller faste kostnader gir FORSØRGER_ELLER_HAR_FASTEKOSTNADER og 100 prosent`() {
        val periode = Periode(rettighetsperiodeFom, rettighetsperiodeTom)
        val input = tomUnderveisInput.copy(
            institusjonsopphold = listOf(
                Institusjonsopphold(
                    periode = Periode(innleggelseStart, innleggelseSlutt),
                    institusjon = Institusjon(
                        erPåInstitusjon = true,
                        skalGiReduksjon = false,
                        skalGiUmiddelbarReduksjon = false
                    )
                )
            )
        )
        val tidligereResultat = Tidslinje(listOf(Segment(periode, lagVurdering())))

        val resultat = regel.vurder(input, tidligereResultat)

        val oppholdSegment =
            resultat.segmenter().first { it.periode.overlapper(Periode(reduksjonStart, innleggelseSlutt)) }
        assertEquals(Prosent.`100_PROSENT`, oppholdSegment.verdi.institusjonVurdering?.grad)
        assertEquals(Årsak.FORSØRGER_ELLER_HAR_FASTEKOSTNADER, oppholdSegment.verdi.institusjonVurdering?.årsak)
    }

    @Test
    fun `opphold med kost og losji gir KOST_OG_LOSJI og 50 prosent etter karanteneperiode`() {
        val periode = Periode(rettighetsperiodeFom, rettighetsperiodeTom)
        val input = tomUnderveisInput.copy(
            institusjonsopphold = listOf(
                Institusjonsopphold(
                    periode = Periode(innleggelseStart, reduksjonStart.minusDays(1)),
                    institusjon = Institusjon(
                        erPåInstitusjon = true,
                        skalGiReduksjon = false,
                        skalGiUmiddelbarReduksjon = false
                    )
                ),
                Institusjonsopphold(
                    periode = Periode(reduksjonStart, innleggelseSlutt),
                    institusjon = Institusjon(
                        erPåInstitusjon = true,
                        skalGiReduksjon = true,
                        skalGiUmiddelbarReduksjon = false
                    )
                )
            )
        )
        val tidligereResultat = Tidslinje(listOf(Segment(periode, lagVurdering())))

        val resultat = regel.vurder(input, tidligereResultat)

        // Perioden før reduksjon starter skal ikke ha institusjonsreduksjon
        val mellomInnleggelseOgReduksjon = resultat.segmenter().first {
            !it.periode.fom.isBefore(innleggelseStart) && it.periode.tom.isBefore(reduksjonStart)
        }
        assertEquals(Prosent.`100_PROSENT`, mellomInnleggelseOgReduksjon.verdi.institusjonVurdering?.grad)
        assertEquals(Årsak.FORSØRGER_ELLER_HAR_FASTEKOSTNADER, mellomInnleggelseOgReduksjon.verdi.institusjonVurdering?.årsak)

        // Perioden etter reduksjon starter skal ha 50% og KOST_OG_LOSJI
        val medReduksjon = resultat.segmenter().first { !it.periode.fom.isBefore(reduksjonStart) }
        assertEquals(Prosent.`50_PROSENT`, medReduksjon.verdi.institusjonVurdering?.grad)
        assertEquals(Årsak.KOST_OG_LOSJI, medReduksjon.verdi.institusjonVurdering?.årsak)
    }

    @Test
    fun `opphold med umiddelbar reduksjon gir reduksjon fra første dag neste måned`() {
        val periode = Periode(rettighetsperiodeFom, rettighetsperiodeTom)
        // Umiddelbar reduksjon: starter 1 måned etter første dag i innleggelsesmåneden
        val umiddelbarReduksjonStart = innleggelseStart.withDayOfMonth(1).plusMonths(1)
        val input = tomUnderveisInput.copy(
            institusjonsopphold = listOf(
                Institusjonsopphold(
                    periode = Periode(innleggelseStart, umiddelbarReduksjonStart.minusDays(1)),
                    institusjon = Institusjon(
                        erPåInstitusjon = true,
                        skalGiReduksjon = false,
                        skalGiUmiddelbarReduksjon = false
                    )
                ),
                Institusjonsopphold(
                    periode = Periode(umiddelbarReduksjonStart, innleggelseSlutt),
                    institusjon = Institusjon(
                        erPåInstitusjon = true,
                        skalGiReduksjon = true,
                        skalGiUmiddelbarReduksjon = true
                    )
                )
            )
        )
        val tidligereResultat = Tidslinje(listOf(Segment(periode, lagVurdering())))

        val resultat = regel.vurder(input, tidligereResultat)

        val medReduksjon = resultat.segmenter().first { !it.periode.fom.isBefore(umiddelbarReduksjonStart) }
        assertEquals(Prosent.`50_PROSENT`, medReduksjon.verdi.institusjonVurdering?.grad)
        assertEquals(Årsak.KOST_OG_LOSJI, medReduksjon.verdi.institusjonVurdering?.årsak)
    }

    @Test
    fun `vurder - tre opphold inkludert forsørger-case`() {
        // Alle datoer er 2 år tilbake i tid for å sikre at perioder alltid er i fortid
        val fom = idag.minusYears(2).withDayOfMonth(5)
        val tom = idag.plusYears(1).withDayOfMonth(1)
        val periode = Periode(fom, tom)

        val opphold1Fom = fom.plusDays(10)  // Innleggelse 1
        val opphold1Tom = fom.plusMonths(6)
        val opphold2Fom = fom.plusMonths(6).plusDays(5)  // Innleggelse 2
        val opphold2Tom = fom.plusMonths(8)
        val opphold3Fom = fom.plusMonths(11).plusDays(9)  // Innleggelse 3 - forsørger
        val opphold3Tom = fom.plusMonths(12).plusDays(15)

        val reduksjonStart1 = opphold1Fom.withDayOfMonth(1).plusMonths(4)
        val reduksjonStart2 = opphold2Fom.withDayOfMonth(1).plusMonths(1)

        val utlederInput = InstitusjonsoppholdInput(
            institusjonsOpphold = listOf(
                Segment(
                    Periode(opphold1Fom, opphold1Tom),
                    no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Institusjon(
                        Institusjonstype.HS, Oppholdstype.D, "123", "opphold1"
                    )
                ),
                Segment(
                    Periode(opphold2Fom, opphold2Tom),
                    no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Institusjon(
                        Institusjonstype.HS, Oppholdstype.D, "456", "opphold2"
                    )
                ),
                Segment(
                    Periode(opphold3Fom, opphold3Tom),
                    no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Institusjon(
                        Institusjonstype.HS, Oppholdstype.D, "789", "opphold3"
                    )
                )
            ),
            soningsvurderinger = emptyList(),
            barnetillegg = emptyList(),
            helsevurderinger = listOf(
                HelseinstitusjonVurdering(
                    periode = Periode(reduksjonStart1, opphold1Tom),
                    begrunnelse = "kost og losji",
                    faarFriKostOgLosji = true,
                    forsoergerEktefelle = false,
                    harFasteUtgifter = false,
                    vurdertIBehandling = BehandlingId(1L),
                    vurdertAv = "ident",
                    vurdertTidspunkt = fom.atStartOfDay()
                ),
                HelseinstitusjonVurdering(
                    periode = Periode(reduksjonStart2, opphold2Tom),
                    begrunnelse = "kost og losji",
                    faarFriKostOgLosji = true,
                    forsoergerEktefelle = false,
                    harFasteUtgifter = false,
                    vurdertIBehandling = BehandlingId(1L),
                    vurdertAv = "ident",
                    vurdertTidspunkt = fom.atStartOfDay()
                ),
                HelseinstitusjonVurdering(
                    periode = Periode(opphold3Fom, opphold3Tom),
                    begrunnelse = "forsørger ektefelle",
                    faarFriKostOgLosji = true,
                    forsoergerEktefelle = true,
                    harFasteUtgifter = false,
                    vurdertIBehandling = BehandlingId(1L),
                    vurdertAv = "ident",
                    vurdertTidspunkt = fom.atStartOfDay()
                ),
            ),
            rettighetsperiode = Periode(fom, fom.plusYears(3))
        )

        val behovForAvklaringer = utlederService.utledBehov(utlederInput)
        val institusjonsOppholdet = MapInstitusjonoppholdTilRegel.map(behovForAvklaringer)

        val input = tomUnderveisInput.copy(institusjonsopphold = institusjonsOppholdet)
        val tidligereResultatTidslinje = Tidslinje(listOf(Segment(periode, lagVurdering())))

        val resultat = regel.vurder(input, tidligereResultatTidslinje)

        // Opphold 1: KOST_OG_LOSJI, 50% etter karanteneperiode
        val medReduksjon1 =
            resultat.segmenter().first { !it.periode.fom.isBefore(reduksjonStart1) && it.periode.tom <= opphold1Tom }
        assertEquals(Prosent.`50_PROSENT`, medReduksjon1.verdi.institusjonVurdering?.grad)
        assertEquals(Årsak.KOST_OG_LOSJI, medReduksjon1.verdi.institusjonVurdering?.årsak)

        // Opphold 2: KOST_OG_LOSJI, 50% etter karanteneperiode
        val medReduksjon2 =
            resultat.segmenter().first { !it.periode.fom.isBefore(reduksjonStart2) && it.periode.tom <= opphold2Tom }
        assertEquals(Prosent.`50_PROSENT`, medReduksjon2.verdi.institusjonVurdering?.grad)
        assertEquals(Årsak.KOST_OG_LOSJI, medReduksjon2.verdi.institusjonVurdering?.årsak)

        // Opphold 3: Når vi utleder behovForAvklaring tar den ikke med perioder for opphold3 siden den ikke gir reduksjon.
        // Da er perioder etter opphold2Tom
        val forsørgerPeriode =
            resultat.segmenter().first { !it.periode.fom.isAfter(opphold2Tom) }
        assertEquals(null, forsørgerPeriode.verdi.institusjonVurdering)
    }
}