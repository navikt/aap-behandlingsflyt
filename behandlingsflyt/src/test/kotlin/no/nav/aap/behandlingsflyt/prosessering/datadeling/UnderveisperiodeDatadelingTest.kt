package no.nav.aap.behandlingsflyt.prosessering.datadeling

import no.nav.aap.behandlingsflyt.behandling.underveis.regler.MeldepliktStatus
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.ArbeidsGradering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.Underveisperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Dagsatser
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.komponenter.verdityper.Prosent.Companion.`0_PROSENT`
import no.nav.aap.komponenter.verdityper.TimerArbeid
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class UnderveisperiodeDatadelingTest {
    @Test
    fun `mapper underveisperiode til datadeling`() {
        val underveisperiode = underveisperiode(
            andelArbeid = Prosent(70),
            grenseverdi = Prosent(60),
            timerArbeidet = BigDecimal("14.5"),
            meldepliktStatus = MeldepliktStatus.MELDT_SEG,
        )

        val dto = underveisperiode.tilDatadeling()

        assertThat(dto.periode).isEqualTo(Periode(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 14)))
        assertThat(dto.meldepliktstatus).isEqualTo("MELDT_SEG")
        assertThat(dto.arbeidsgrad).isEqualTo(70)
        assertThat(dto.overgrenseVerdi).isTrue()
        assertThat(dto.timerArbeidet).isEqualByComparingTo(BigDecimal("14.5"))
    }

    @Test
    fun `mapper underveisperiode uten meldepliktstatus og under grenseverdi`() {
        val underveisperiode = underveisperiode(
            andelArbeid = Prosent(50),
            grenseverdi = Prosent(60),
            timerArbeidet = BigDecimal.ZERO,
            meldepliktStatus = null,
        )

        val dto = underveisperiode.tilDatadeling()

        assertThat(dto.meldepliktstatus).isNull()
        assertThat(dto.arbeidsgrad).isEqualTo(50)
        assertThat(dto.overgrenseVerdi).isFalse()
        assertThat(dto.timerArbeidet).isEqualByComparingTo(BigDecimal.ZERO)
    }

    private fun underveisperiode(
        andelArbeid: Prosent,
        grenseverdi: Prosent,
        timerArbeidet: BigDecimal,
        meldepliktStatus: MeldepliktStatus?,
    ) = Underveisperiode(
        periode = Periode(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 14)),
        meldePeriode = Periode(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 14)),
        utfall = Utfall.OPPFYLT,
        rettighetsType = RettighetsType.BISTANDSBEHOV,
        avslagsårsak = null,
        grenseverdi = grenseverdi,
        institusjonsoppholdReduksjon = `0_PROSENT`,
        arbeidsgradering = ArbeidsGradering(
            totaltAntallTimer = TimerArbeid(timerArbeidet),
            andelArbeid = andelArbeid,
            fastsattArbeidsevne = `0_PROSENT`,
            gradering = `0_PROSENT`,
            opplysningerMottatt = null,
        ),
        trekk = Dagsatser(0),
        brukerAvKvoter = emptySet(),
        meldepliktStatus = meldepliktStatus,
        meldepliktGradering = null,
    )
}
