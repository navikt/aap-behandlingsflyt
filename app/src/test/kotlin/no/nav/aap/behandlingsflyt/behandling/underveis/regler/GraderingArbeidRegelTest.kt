package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsevne.ArbeidsevneGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsevne.ArbeidsevneVurdering
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.tidslinje.Tidslinje
import no.nav.aap.verdityper.Prosent
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

import java.time.LocalDate
import java.time.LocalDateTime

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

    private fun vurder(input: UnderveisInput): Tidslinje<Vurdering> {
        return regel.vurder(
            input,
            MeldepliktRegel().vurder(input, Tidslinje())
        )
    }
}