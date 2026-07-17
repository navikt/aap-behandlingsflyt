package no.nav.aap.behandlingsflyt.behandling.beregning

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.InntektPerÅr
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.ManuellInntektVurdering
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.komponenter.verdityper.Bruker
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.Year

class KombinerInntektOgManuellInntektTest {

    @Test
    fun `manuell årsinntekt fyller inn år som mangler fra register`() {
        val register = setOf(InntektPerÅr(2022, Beløp(200_000)))
        val manuell = setOf(
            ManuellInntektVurdering(
                år = Year.of(2021),
                begrunnelse = "manglet i register",
                belop = Beløp(100_000),
                vurdertAv = Bruker("saksbehandler"),
            )
        )

        val resultat = Beregning.kombinerInntektOgManuellInntekt(register, manuell)

        assertThat(resultat).containsExactlyInAnyOrder(
            InntektPerÅr(2021, Beløp(100_000)),
            InntektPerÅr(2022, Beløp(200_000)),
        )
    }

    @Test
    fun `eøs-beløp legges til registerets årsinntekt`() {
        val register = setOf(InntektPerÅr(2022, Beløp(200_000)))
        val manuell = setOf(
            ManuellInntektVurdering(
                år = Year.of(2022),
                begrunnelse = "eøs-inntekt",
                belop = Beløp(999),
                eøsBeløp = Beløp(50_000),
                vurdertAv = Bruker("saksbehandler"),
            )
        )

        val resultat = Beregning.kombinerInntektOgManuellInntekt(register, manuell)

        // Dagens oppførsel for år-nivå: register vinner på belop, eøs legges til.
        assertThat(resultat).containsExactly(InntektPerÅr(2022, Beløp(250_000)))
    }

    @Test
    fun `delperioder for samme år summeres og overstyrer registerets årsinntekt`() {
        val register = setOf(InntektPerÅr(2022, Beløp(640_500)))
        val manuell = setOf(
            ManuellInntektVurdering(
                år = Year.of(2022),
                begrunnelse = "endring i uføregrad",
                belop = Beløp(100_000),
                vurdertAv = Bruker("saksbehandler"),
                månedsPeriode = Periode(LocalDate.of(2022, 1, 1), LocalDate.of(2022, 2, 28)),
            ),
            ManuellInntektVurdering(
                år = Year.of(2022),
                begrunnelse = "endring i uføregrad",
                belop = Beløp(540_500),
                eøsBeløp = Beløp(35_000),
                vurdertAv = Bruker("saksbehandler"),
                månedsPeriode = Periode(LocalDate.of(2022, 3, 1), LocalDate.of(2022, 12, 31)),
            ),
        )

        val resultat = Beregning.kombinerInntektOgManuellInntekt(register, manuell)

        // (100 000 + 540 500) + 35 000 EØS = 675 500 («Totalt» i kortet), overstyrer 640 500 fra register.
        assertThat(resultat).containsExactly(InntektPerÅr(2022, Beløp(675_500)))
    }
}
