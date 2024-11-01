package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.ArbeidIPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Pliktkort
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsevne.ArbeidsevneGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsevne.ArbeidsevneVurdering
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.tidslinje.Tidslinje
import no.nav.aap.verdityper.Prosent
import no.nav.aap.verdityper.TimerArbeid
import no.nav.aap.verdityper.dokument.JournalpostId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class GraderingArbeidRegelTest {
    private val regel = GraderingArbeidRegel()

    @Test
    fun `Arbeidsevnevurdering fører ikke til vurdering utenfor rettighetsperioden`() {
        val fom = LocalDate.parse("2024-10-31")
        val rettighetsperiode = Periode(fom, LocalDate.parse("2025-10-31"))
        val input = tomUnderveisInput.copy(
            rettighetsperiode = rettighetsperiode,
            arbeidsevneGrunnlag = ArbeidsevneGrunnlag(
                listOf(
                    ArbeidsevneVurdering(
                        begrunnelse = "",
                        arbeidsevne = Prosent.`50_PROSENT`,
                        fraDato = fom.minusDays(1), /* viktig at vi tester vurderinger fra før rettighetsperioden */
                        opprettetTid = LocalDateTime.now(),
                    )
                )
            )
        )
        val vurdering = vurder(input)

        assertTrue(rettighetsperiode.inneholder(vurdering.helePerioden()))
    }

    @Test
    fun `Hvis fastsatt arbeidsevne er lavere enn timer arbeidet, blir timer arbeidet brukt i utregning av gradering`() {
        val rettighetsperiode = Periode(LocalDate.parse("2024-10-31"), LocalDate.parse("2025-10-31"))
        val periode = Periode(rettighetsperiode.fom, rettighetsperiode.fom.plusDays(13))
        val input = underveisInput(
            rettighetsperiode = rettighetsperiode,
            periodeHvorArbeidet = periode,
            fastsattArbeidsevne = Prosent.`50_PROSENT`,
            timerArbeidet = BigDecimal(10)
        )
        val vurdering = vurder(input)

        assertEquals(Prosent.`50_PROSENT`, vurdering.segment(rettighetsperiode.fom)?.verdi?.gradering()?.gradering)
    }

    @Test
    fun `Hvis timer arbeidet er lavere enn fastsatt arbeidsevne, blir fastsatt arbeidsevne brukt i utregning av gradering`() {
        val rettighetsperiode = Periode(LocalDate.parse("2024-10-31"), LocalDate.parse("2025-10-31"))
        val periode = Periode(rettighetsperiode.fom, rettighetsperiode.fom.plusDays(13))
        val input = underveisInput(
            rettighetsperiode = rettighetsperiode,
            periodeHvorArbeidet = periode,
            fastsattArbeidsevne = Prosent.`30_PROSENT`,
            timerArbeidet = BigDecimal(37.5)
        )
        val vurdering = vurder(input)

        assertEquals(Prosent.`50_PROSENT`, vurdering.segment(rettighetsperiode.fom)?.verdi?.gradering()?.gradering)
    }

    @Test
    fun `Hvis det ikke er registrert timer arbeidet brukes fastsatt arbeidsevne i utregning av gradering`() {
        val rettighetsperiode = Periode(LocalDate.parse("2024-10-31"), LocalDate.parse("2025-10-31"))
        val periode = Periode(rettighetsperiode.fom, rettighetsperiode.fom.plusDays(13))
        val input = underveisInput(
            rettighetsperiode = rettighetsperiode,
            periodeHvorArbeidet = periode,
            fastsattArbeidsevne = Prosent.`30_PROSENT`,
            timerArbeidet = null
        )
        val vurdering = vurder(input)

        assertEquals(Prosent.`70_PROSENT`, vurdering.segment(rettighetsperiode.fom)?.verdi?.gradering()?.gradering)
    }

    @Test
    fun `Hvis det ikke er registrert arbeidsevne brukes timer arbeidet i utregning av gradering`() {
        val rettighetsperiode = Periode(LocalDate.parse("2024-10-31"), LocalDate.parse("2025-10-31"))
        val periode = Periode(rettighetsperiode.fom, rettighetsperiode.fom.plusDays(13))
        val input = underveisInput(
            rettighetsperiode = rettighetsperiode,
            periodeHvorArbeidet = periode,
            fastsattArbeidsevne = null,
            timerArbeidet = BigDecimal(37.5)
        )
        val vurdering = vurder(input)

        assertEquals(Prosent.`50_PROSENT`, vurdering.segment(rettighetsperiode.fom)?.verdi?.gradering()?.gradering)
    }

    @Test
    fun `Jobber over 100 prosent`() {
        val rettighetsperiode = Periode(LocalDate.parse("2024-10-31"), LocalDate.parse("2025-10-31"))
        val periode = Periode(rettighetsperiode.fom, rettighetsperiode.fom.plusDays(13))
        val input = underveisInput(
            rettighetsperiode = rettighetsperiode,
            periodeHvorArbeidet = periode,
            fastsattArbeidsevne = null,
            timerArbeidet = BigDecimal(100)
        )
        val vurdering = vurder(input)

        assertEquals(Prosent.`0_PROSENT`, vurdering.segment(rettighetsperiode.fom)?.verdi?.gradering()?.gradering)
    }

    private fun underveisInput(
        rettighetsperiode: Periode,
        periodeHvorArbeidet: Periode,
        fastsattArbeidsevne: Prosent?,
        timerArbeidet: BigDecimal?
    ) = tomUnderveisInput.copy(
        rettighetsperiode = rettighetsperiode,
        pliktkort = listOfNotNull(timerArbeidet?.let {
            Pliktkort(
                journalpostId = JournalpostId(""),
                timerArbeidPerPeriode = setOf(
                    ArbeidIPeriode(periodeHvorArbeidet, TimerArbeid(timerArbeidet))
                )
            )
        }),
        arbeidsevneGrunnlag = ArbeidsevneGrunnlag(
            listOfNotNull(fastsattArbeidsevne?.let {
                ArbeidsevneVurdering(
                    begrunnelse = "",
                    arbeidsevne = it,
                    fraDato = rettighetsperiode.fom,
                    opprettetTid = LocalDateTime.now(),
                )
            })
        )
    )

    private fun vurder(input: UnderveisInput): Tidslinje<Vurdering> {
        return regel.vurder(
            input,
            MeldepliktRegel().vurder(input, Tidslinje())
        ).mapValue { it.copy(vurderinger = EnumMap(mapOf(Vilkårtype.ALDERSVILKÅRET to Utfall.OPPFYLT))) }
    }
}