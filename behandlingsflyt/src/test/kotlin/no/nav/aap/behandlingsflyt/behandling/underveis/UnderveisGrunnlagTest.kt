package no.nav.aap.behandlingsflyt.behandling.underveis

import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Hverdager
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.ArbeidsGradering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.Underveisperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisÅrsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Dagsatser
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.komponenter.verdityper.TimerArbeid
import org.junit.jupiter.api.Test
import org.assertj.core.api.Assertions.assertThat
import java.math.BigDecimal
import java.time.LocalDate

private val FØRSTE_JAN_2026 = LocalDate.of(2026, 1, 1)
private val TOM_PERIODE = Periode(FØRSTE_JAN_2026, FØRSTE_JAN_2026)
private val dagensDato = LocalDate.now()

class UnderveisGrunnlagTest {
    @Test
    fun `skal utlede fulle kvoter ved avslåtte perioder`() {
        val perioder = listOf(
            underveisperiode(TOM_PERIODE, RettighetsType.BISTANDSBEHOV, UnderveisÅrsak.VARIGHETSKVOTE_BRUKT_OPP),
            underveisperiode(TOM_PERIODE, RettighetsType.SYKEPENGEERSTATNING, UnderveisÅrsak.MELDEPLIKT_FRIST_IKKE_PASSERT)
        )

        val underveisGrunnlag = UnderveisGrunnlag(1234, perioder)
        val kvoter = underveisGrunnlag.utledKvoterForRettighetstype(RettighetsType.BISTANDSBEHOV)
        val totalKvoteForBistandsbehov = 784

        assertThat(kvoter.bruktKvote).isEqualTo(0)
        assertThat(kvoter.gjenværendeKvote).isEqualTo(totalKvoteForBistandsbehov)
    }

    @Test
    fun `skal utlede reduserte kvoter ved innvilgede perioder`() {
        val historiskPeriode = underveisperiode(
            Periode(FØRSTE_JAN_2026.minusDays(10), FØRSTE_JAN_2026.minusDays(4)),
            RettighetsType.SYKEPENGEERSTATNING, null)
        val gjeldendePeriode = underveisperiode(
            Periode(FØRSTE_JAN_2026.minusDays(3), FØRSTE_JAN_2026.plusWeeks(1)),
            RettighetsType.SYKEPENGEERSTATNING, null)

        val perioder = listOf(historiskPeriode, gjeldendePeriode)
        val underveisGrunnlag = UnderveisGrunnlag(1234, perioder)
        val kvoter = underveisGrunnlag.utledKvoterForRettighetstype(RettighetsType.SYKEPENGEERSTATNING)
        val totalKvoteForSykepengeErstatning = 130
        val forventetBruktKvote = 14

        assertThat(kvoter.bruktKvote).isEqualTo(forventetBruktKvote)
        assertThat(kvoter.gjenværendeKvote).isEqualTo(totalKvoteForSykepengeErstatning.minus(forventetBruktKvote.minus(1)))
    }

    @Test
    fun `skal utlede maksdato 773 hverdager frem i tid for rettighet bistandsbehov ved brukt kvote på 2 uker`() {
        val perioder = listOf(
            underveisperiode(
                Periode(FØRSTE_JAN_2026.minusWeeks(3), FØRSTE_JAN_2026.minusWeeks(1)),
                RettighetsType.BISTANDSBEHOV, null)
        )

        val underveisGrunnlag = UnderveisGrunnlag(1234, perioder)
        val maksdato = underveisGrunnlag.utledMaksdatoForRettighet(RettighetsType.BISTANDSBEHOV)
        val forventetMaksdato = Hverdager(773).fraOgMed(dagensDato)

        assertThat(maksdato).isEqualTo(forventetMaksdato)
    }

    @Test
    fun `skal utlede maksdato 115 hverdager frem i tid for rettighet sykepengeerstatning ved brukt kvote på 3 uker`() {
        val perioder = listOf(
            underveisperiode(
                Periode(FØRSTE_JAN_2026.minusWeeks(5), FØRSTE_JAN_2026.minusWeeks(2)),
                RettighetsType.SYKEPENGEERSTATNING, null)
        )

        val underveisGrunnlag = UnderveisGrunnlag(1234, perioder)
        val maksdato = underveisGrunnlag.utledMaksdatoForRettighet(RettighetsType.SYKEPENGEERSTATNING)
        val forventetMaksdato = Hverdager(115).fraOgMed(dagensDato)

        assertThat(maksdato).isEqualTo(forventetMaksdato)
    }
}

private fun underveisperiode(
    periode: Periode,
    rettighetsType: RettighetsType,
    avslagsÅrsak: UnderveisÅrsak? = null,
): Underveisperiode {
    return Underveisperiode(
        periode = periode,
        meldePeriode = periode,
        utfall = Utfall.OPPFYLT,
        rettighetsType = rettighetsType,
        avslagsårsak = avslagsÅrsak,
        grenseverdi = Prosent.`100_PROSENT`,
        institusjonsoppholdReduksjon = Prosent.`0_PROSENT`,
        arbeidsgradering = ArbeidsGradering(
            totaltAntallTimer = TimerArbeid(BigDecimal(0)),
            andelArbeid = Prosent.`0_PROSENT`,
            fastsattArbeidsevne = Prosent.`100_PROSENT`,
            gradering = Prosent.`100_PROSENT`,
            opplysningerMottatt = null,
        ),
        trekk = Dagsatser(0),
        brukerAvKvoter = emptySet(),
        meldepliktStatus = null,
        meldepliktGradering = Prosent.`0_PROSENT`,
    )
}
