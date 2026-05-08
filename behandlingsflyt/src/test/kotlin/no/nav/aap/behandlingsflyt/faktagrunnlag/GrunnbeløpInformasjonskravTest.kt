package no.nav.aap.behandlingsflyt.faktagrunnlag

import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.GraderingGrunnlag
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.Minstesats
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.Tilkjent
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.TilkjentYtelsePeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.Grunnbeløp
import no.nav.aap.behandlingsflyt.help.flytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.help.tomtTilkjentYtelseGrunnlag
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.behandlingsflyt.test.FakeUnleashBaseWithDefaultDisabled
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryTilkjentYtelseRepository
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.behandlingsflyt.test.juni
import no.nav.aap.behandlingsflyt.test.mai
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.komponenter.verdityper.GUnit
import no.nav.aap.komponenter.verdityper.Prosent.Companion.`0_PROSENT`
import no.nav.aap.komponenter.verdityper.Prosent.Companion.`100_PROSENT`
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class GrunnbeløpInformasjonskravTest {

    private val unleash = FakeUnleashBaseWithDefaultDisabled(
        listOf(BehandlingsflytFeature.GrunnbeloepInformasjonskrav)
    )

    private lateinit var informasjonskrav: GrunnbeløpInformasjonskrav

    @BeforeEach
    fun setup() {
        InMemoryTilkjentYtelseRepository.slett(kontekst().behandlingId)
        informasjonskrav = GrunnbeløpInformasjonskrav(
            tilkjentYtelseRepository = InMemoryTilkjentYtelseRepository,
            unleashGateway = unleash,
        )
    }

    @Test
    fun `skal returnere IKKE_ENDRET når ingen tilkjent ytelse finnes`() {
        val resultat = informasjonskrav.oppdater(IngenInput, IngenRegisterData, kontekst())

        assertThat(resultat).isEqualTo(Informasjonskrav.Endret.IKKE_ENDRET)
    }

    @Test
    fun `skal returnere IKKE_ENDRET når alle perioder har gjeldende grunnbeløp`() {
        val periodeFom = 1 juni 2025
        val periodeTom = LocalDate.of(2025, 12, 31)
        val gjeldendeG = Grunnbeløp.finnGrunnbeløp(periodeFom)

        val kontekst = kontekst()
        lagreTilkjentYtelse(kontekst, periodeFom, periodeTom, gjeldendeG)

        val resultat = informasjonskrav.oppdater(IngenInput, IngenRegisterData, kontekst)

        assertThat(resultat).isEqualTo(Informasjonskrav.Endret.IKKE_ENDRET)
    }

    @Test
    fun `skal returnere ENDRET når en periode har utdatert grunnbeløp`() {
        val periodeFom = 1 juni 2025
        val periodeTom = LocalDate.of(2025, 12, 31)
        val utdatertG = Beløp(124_028) // G fra 2024

        val kontekst = kontekst()
        lagreTilkjentYtelse(kontekst, periodeFom, periodeTom, utdatertG)

        val resultat = informasjonskrav.oppdater(IngenInput, IngenRegisterData, kontekst)

        assertThat(resultat).isEqualTo(Informasjonskrav.Endret.ENDRET)
    }

    @Test
    fun `skal returnere ENDRET når minst én av flere perioder har utdatert grunnbeløp`() {
        val kontekst = kontekst()

        val periode1Fom = 1 januar 2025
        val periode1Tom = LocalDate.of(2025, 4, 30)
        val gForPeriode1 = Grunnbeløp.finnGrunnbeløp(periode1Fom)

        val periode2Fom = 1 mai 2025
        val periode2Tom = LocalDate.of(2025, 12, 31)
        val utdatertG = Beløp(124_028) // G fra 2024, men perioden starter etter 1. mai 2025

        InMemoryTilkjentYtelseRepository.lagre(
            kontekst.behandlingId,
            listOf(
                lagTilkjentYtelsePeriode(periode1Fom, periode1Tom, gForPeriode1),
                lagTilkjentYtelsePeriode(periode2Fom, periode2Tom, utdatertG),
            ),
            tomtTilkjentYtelseGrunnlag,
            ""
        )

        val resultat = informasjonskrav.oppdater(IngenInput, IngenRegisterData, kontekst)

        assertThat(resultat).isEqualTo(Informasjonskrav.Endret.ENDRET)
    }

    @Test
    fun `skal ikke være relevant når feature toggle er avskrudd`() {
        val disabledUnleash = FakeUnleashBaseWithDefaultDisabled(emptyList())
        val krav = GrunnbeløpInformasjonskrav(
            tilkjentYtelseRepository = InMemoryTilkjentYtelseRepository,
            unleashGateway = disabledUnleash,
        )

        val erRelevant = krav.erRelevant(kontekst(), no.nav.aap.behandlingsflyt.kontrakt.steg.StegType.BEREGN_TILKJENT_YTELSE, null)

        assertThat(erRelevant).isFalse()
    }

    private fun kontekst() = flytKontekstMedPerioder {
        vurderingType = VurderingType.REVURDERING
    }

    private fun lagreTilkjentYtelse(
        kontekst: no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder,
        fom: LocalDate,
        tom: LocalDate,
        grunnbeløp: Beløp
    ) {
        InMemoryTilkjentYtelseRepository.lagre(
            kontekst.behandlingId,
            listOf(lagTilkjentYtelsePeriode(fom, tom, grunnbeløp)),
            tomtTilkjentYtelseGrunnlag,
            ""
        )
    }

    private fun lagTilkjentYtelsePeriode(
        fom: LocalDate,
        tom: LocalDate,
        grunnbeløp: Beløp
    ): TilkjentYtelsePeriode {
        return TilkjentYtelsePeriode(
            periode = Periode(fom, tom),
            tilkjent = Tilkjent(
                dagsats = Beløp(1000),
                gradering = `100_PROSENT`,
                graderingGrunnlag = GraderingGrunnlag(
                    samordningGradering = `0_PROSENT`,
                    institusjonGradering = `0_PROSENT`,
                    arbeidGradering = `0_PROSENT`,
                    samordningUføregradering = `0_PROSENT`,
                    samordningArbeidsgiverGradering = `0_PROSENT`,
                    meldepliktGradering = `0_PROSENT`,
                ),
                grunnlagsfaktor = GUnit(java.math.BigDecimal("2.0")),
                grunnbeløp = grunnbeløp,
                antallBarn = 0,
                barnetilleggsats = Beløp(0),
                barnetillegg = Beløp(0),
                barnepensjonDagsats = Beløp(0),
                utbetalingsdato = fom,
                minsteSats = Minstesats.IKKE_MINSTESATS,
                redusertDagsats = null,
            )
        )
    }
}
