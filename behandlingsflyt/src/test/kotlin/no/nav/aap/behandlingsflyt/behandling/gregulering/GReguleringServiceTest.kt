package no.nav.aap.behandlingsflyt.behandling.gregulering

import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.GraderingGrunnlag
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.Minstesats
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.Tilkjent
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.TilkjentYtelsePeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.Faktagrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.ArbeidsGradering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.Underveisperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.Grunnbeløp
import no.nav.aap.behandlingsflyt.help.flytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.help.tomtTilkjentYtelseGrunnlag
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryTilkjentYtelseRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryUnderveisRepository
import no.nav.aap.behandlingsflyt.test.april
import no.nav.aap.behandlingsflyt.test.desember
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.behandlingsflyt.test.juli
import no.nav.aap.behandlingsflyt.test.juni
import no.nav.aap.behandlingsflyt.test.mai
import no.nav.aap.behandlingsflyt.test.mars
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.komponenter.verdityper.Dagsatser
import no.nav.aap.komponenter.verdityper.GUnit
import no.nav.aap.komponenter.verdityper.Prosent.Companion.`0_PROSENT`
import no.nav.aap.komponenter.verdityper.Prosent.Companion.`100_PROSENT`
import no.nav.aap.komponenter.verdityper.TimerArbeid
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class GReguleringServiceTest {

    private lateinit var service: GReguleringService

    @BeforeEach
    fun setup() {
        val kontekst = kontekst()
        InMemoryTilkjentYtelseRepository.slett(kontekst.behandlingId)
        InMemoryUnderveisRepository.slett(kontekst.behandlingId)
        service = GReguleringService(
            underveisRepository = InMemoryUnderveisRepository,
            tilkjentYtelseRepository = InMemoryTilkjentYtelseRepository,
        )
    }

    @Test
    fun `skal returnere false når ingen tilkjent ytelse finnes`() {
        val resultat = service.erGrunnbeløpEndretForBehandling(kontekst().behandlingId)

        assertThat(resultat).isFalse()
    }

    @Test
    fun `skal returnere false når alle perioder har gjeldende grunnbeløp`() {
        val periodeFom = 1 juni 2025
        val periodeTom = 31 desember 2025
        val gjeldendeG = Grunnbeløp.finnGrunnbeløp(periodeFom)

        val kontekst = kontekst()
        lagreTilkjentYtelse(kontekst, periodeFom, periodeTom, gjeldendeG)
        lagreOppfyltUnderveis(kontekst, periodeFom, periodeTom)

        val resultat = service.erGrunnbeløpEndretForBehandling(kontekst.behandlingId)

        assertThat(resultat).isFalse()
    }

    @Test
    fun `skal returnere true når en periode har utdatert grunnbeløp`() {
        val periodeFom = 1 juni 2025
        val periodeTom = 31 desember 2025
        val utdatertG = Beløp(124_028)

        val kontekst = kontekst()
        lagreTilkjentYtelse(kontekst, periodeFom, periodeTom, utdatertG)
        lagreOppfyltUnderveis(kontekst, periodeFom, periodeTom)

        val resultat = service.erGrunnbeløpEndretForBehandling(kontekst.behandlingId)

        assertThat(resultat).isTrue()
    }

    @Test
    fun `skal returnere true når minst én av flere perioder har utdatert grunnbeløp`() {
        val kontekst = kontekst()

        val periode1Fom = 1 januar 2025
        val periode1Tom = 30 april 2025
        val gForPeriode1 = Grunnbeløp.finnGrunnbeløp(periode1Fom)

        val periode2Fom = 1 mai 2025
        val periode2Tom = 31 desember 2025
        val utdatertG = Beløp(124_028)

        InMemoryTilkjentYtelseRepository.lagre(
            kontekst.behandlingId,
            listOf(
                lagTilkjentYtelsePeriode(periode1Fom, periode1Tom, gForPeriode1),
                lagTilkjentYtelsePeriode(periode2Fom, periode2Tom, utdatertG),
            ),
            tomtTilkjentYtelseGrunnlag,
            ""
        )
        lagreOppfyltUnderveis(kontekst, periode1Fom, periode2Tom)

        val resultat = service.erGrunnbeløpEndretForBehandling(kontekst.behandlingId)

        assertThat(resultat).isTrue()
    }

    @Test
    fun `skal returnere true når periode krysser G-grensen med gammelt grunnbeløp`() {
        val periodeFom = 1 mars 2025
        val periodeTom = 1 juli 2025
        val gammeltG = Grunnbeløp.finnGrunnbeløp(periodeFom)

        val kontekst = kontekst()
        lagreTilkjentYtelse(kontekst, periodeFom, periodeTom, gammeltG)
        lagreOppfyltUnderveis(kontekst, periodeFom, periodeTom)

        val resultat = service.erGrunnbeløpEndretForBehandling(kontekst.behandlingId)

        assertThat(resultat).isTrue()
    }

    @Test
    fun `skal returnere true når periode krysser G-grensen med nytt grunnbeløp`() {
        val periodeFom = 1 mars 2025
        val periodeTom = 1 juli 2025
        val nyttG = Grunnbeløp.finnGrunnbeløp(1 mai 2025)

        val kontekst = kontekst()
        lagreTilkjentYtelse(kontekst, periodeFom, periodeTom, nyttG)
        lagreOppfyltUnderveis(kontekst, periodeFom, periodeTom)

        val resultat = service.erGrunnbeløpEndretForBehandling(kontekst.behandlingId)

        assertThat(resultat).isTrue()
    }

    @Test
    fun `skal returnere false når G er utdatert men underveis er ikke oppfylt`() {
        val periodeFom = 1 juni 2025
        val periodeTom = 31 desember 2025
        val utdatertG = Beløp(124_028)

        val kontekst = kontekst()
        lagreTilkjentYtelse(kontekst, periodeFom, periodeTom, utdatertG)
        lagreIkkeOppfyltUnderveis(kontekst, periodeFom, periodeTom)

        val resultat = service.erGrunnbeløpEndretForBehandling(kontekst.behandlingId)

        assertThat(resultat).isFalse()
    }

    @Test
    fun `skal returnere true når rettighet spenner over G-regulering`() {
        val periodeFom = 1 april 2025
        val periodeTom = 31 desember 2025

        val kontekst = kontekst()
        lagreOppfyltUnderveis(kontekst, periodeFom, periodeTom)

        val resultat = service.erGrunnbeløpEndretForRettighetsTypeTidslinje(kontekst.behandlingId)

        assertThat(resultat).isTrue()
    }

    @Test
    fun `skal returnere false når rettighet ikke spenner over G-regulering`() {
        val periodeFom = 1 mai 2025
        val periodeTom = 31 desember 2025

        val kontekst = kontekst()
        lagreOppfyltUnderveis(kontekst, periodeFom, periodeTom)

        val resultat = service.erGrunnbeløpEndretForRettighetsTypeTidslinje(kontekst.behandlingId)

        assertThat(resultat).isFalse()
    }

    private fun kontekst() = flytKontekstMedPerioder {
        vurderingType = VurderingType.REVURDERING
    }

    private fun lagreTilkjentYtelse(
        kontekst: FlytKontekstMedPerioder,
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

    private fun lagreOppfyltUnderveis(
        kontekst: FlytKontekstMedPerioder,
        fom: LocalDate,
        tom: LocalDate
    ) {
        InMemoryUnderveisRepository.lagre(
            kontekst.behandlingId,
            listOf(lagUnderveisperiode(fom, tom, Utfall.OPPFYLT)),
            input = object : Faktagrunnlag {}
        )
    }

    private fun lagreIkkeOppfyltUnderveis(
        kontekst: FlytKontekstMedPerioder,
        fom: LocalDate,
        tom: LocalDate
    ) {
        InMemoryUnderveisRepository.lagre(
            kontekst.behandlingId,
            listOf(lagUnderveisperiode(fom, tom, Utfall.IKKE_OPPFYLT)),
            input = object : Faktagrunnlag {}
        )
    }

    private fun lagUnderveisperiode(
        fom: LocalDate,
        tom: LocalDate,
        utfall: Utfall
    ): Underveisperiode {
        return Underveisperiode(
            periode = Periode(fom, tom),
            meldePeriode = Periode(fom, tom),
            utfall = utfall,
            rettighetsType = if (utfall == Utfall.OPPFYLT) RettighetsType.BISTANDSBEHOV else null,
            avslagsårsak = if (utfall == Utfall.IKKE_OPPFYLT) no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisÅrsak.IKKE_GRUNNLEGGENDE_RETT else null,
            grenseverdi = `100_PROSENT`,
            institusjonsoppholdReduksjon = `0_PROSENT`,
            arbeidsgradering = ArbeidsGradering(
                totaltAntallTimer = TimerArbeid(BigDecimal.ZERO),
                andelArbeid = `0_PROSENT`,
                fastsattArbeidsevne = `100_PROSENT`,
                gradering = `100_PROSENT`,
                opplysningerMottatt = null,
            ),
            trekk = Dagsatser(0),
            brukerAvKvoter = emptySet(),
            meldepliktStatus = null,
            meldepliktGradering = `0_PROSENT`,
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
                grunnlagsfaktor = GUnit(BigDecimal("2.0")),
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
