package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.behandlingsflyt.behandling.etannetsted.EtAnnetStedInput
import no.nav.aap.behandlingsflyt.behandling.etannetsted.EtAnnetStedUtlederService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.ArbeidsGradering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisÅrsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Institusjon
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Institusjonstype
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Oppholdstype
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.Soningsvurdering
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.barnetillegg.BarnetilleggRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.register.institusjonsopphold.InstitusjonsoppholdRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
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

class SoningRegelTest {

    private val mockConnection = MockConnection().toDBConnection()
    val utlederService = EtAnnetStedUtlederService(
        BarnetilleggRepositoryImpl(mockConnection),
        InstitusjonsoppholdRepositoryImpl(mockConnection),
        SakRepositoryImpl(mockConnection),
        BehandlingRepositoryImpl(mockConnection)
    )

    val regel = SoningRegel()


    @Test
    fun vurder() {
        val periode = Periode(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 1))
        val vurderingFraTidligereResultat = Vurdering(
            RettighetsType.BISTANDSBEHOV, MeldepliktVurdering.Fritak,
            ArbeidsGradering(
                totaltAntallTimer = TimerArbeid(BigDecimal.ZERO),
                andelArbeid = Prosent(0),
                fastsattArbeidsevne = Prosent.`0_PROSENT`,
                gradering = Prosent.`100_PROSENT`,
                opplysningerMottatt = null,
            ), grenseverdi = Prosent(60)
        )
        val tidligereResultatTidslinje = Tidslinje(listOf(Segment(periode, vurderingFraTidligereResultat)))

        val utlederInput = EtAnnetStedInput(
            institusjonsOpphold = listOf(
                Segment(
                    Periode(LocalDate.of(2024, 1, 6), (LocalDate.of(2024, 2, 15))),
                    Institusjon(
                        Institusjonstype.FO,
                        Oppholdstype.S,
                        "123123123",
                        "test fengsel"
                    )
                )
            ),
            soningsvurderinger = listOf(
                Soningsvurdering(
                    skalOpphøre = true,
                    begrunnelse = "Formue under forvaring",
                    fraDato = LocalDate.of(2024, 1, 6)
                ),
                Soningsvurdering(
                    skalOpphøre = true,
                    begrunnelse = "Soner i fengsel",
                    fraDato = LocalDate.of(2024, 1, 11)
                ),
                Soningsvurdering(
                    skalOpphøre = false,
                    begrunnelse = "Jobber utenfor anstalten",
                    fraDato = LocalDate.of(2024, 1, 16)
                ),
                Soningsvurdering(
                    skalOpphøre = false,
                    begrunnelse = "Fotlenke",
                    fraDato = LocalDate.of(2024, 2, 6)
                ),
            ),
            barnetillegg = emptyList(),
            helsevurderinger = emptyList(),
            rettighetsperiode = Periode(periode.fom, periode.fom.plusYears(3))
        )

        val delresultat = utlederService.utledBehov(utlederInput)

        val soningOppholdet = MapInstitusjonoppholdTilRegel.map(delresultat)

        val input = tomUnderveisInput.copy(
            etAnnetSted = soningOppholdet,
        )

        val resultat = regel.vurder(input, tidligereResultatTidslinje)

        assertEquals(3, resultat.count())

        //Soner ikke
        assertEquals(
            Periode(LocalDate.of(2024, 1, 1), (LocalDate.of(2024, 1, 5))),
            resultat.segmenter().elementAt(0).periode
        )
        assertEquals(Prosent.`100_PROSENT`, resultat.segmenter().elementAt(0).verdi.arbeidsgradering().gradering)
        assertEquals(null, resultat.segmenter().elementAt(0).verdi.avslagsårsak())

        //Formue under forvaltning og soner i fengsel
        assertEquals(
            Periode(LocalDate.of(2024, 1, 6), (LocalDate.of(2024, 1, 15))),
            resultat.segmenter().elementAt(1).periode
        )
        assertEquals(UnderveisÅrsak.SONER_STRAFF, resultat.segmenter().elementAt(1).verdi.avslagsårsak())

        // Arbeider utenfor anstalten og soner i ved frigang
        assertEquals(
            Periode(LocalDate.of(2024, 1, 16), (LocalDate.of(2024, 12, 1))),
            resultat.segmenter().elementAt(2).periode
        )
        assertEquals(Prosent.`100_PROSENT`, resultat.segmenter().elementAt(2).verdi.arbeidsgradering().gradering)

    }
}