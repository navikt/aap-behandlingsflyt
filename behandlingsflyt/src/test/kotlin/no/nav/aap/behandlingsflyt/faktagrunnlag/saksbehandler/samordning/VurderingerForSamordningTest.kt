package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.samordning

import no.nav.aap.behandlingsflyt.behandling.samordning.Ytelse
import no.nav.aap.komponenter.type.Periode
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Test
import java.time.LocalDate

class VurderingerForSamordningTest {
    @Test
    fun `skal kaste exception når perioder for samme ytelsetype overlapper`() {
        org.junit.jupiter.api.assertThrows<IllegalArgumentException> {
            VurderingerForSamordning(
                begrunnelse = "...",
                maksDatoEndelig = false,
                fristNyRevurdering = LocalDate.now(),
                vurderteSamordningerData = listOf(
                    SamordningVurderingData(
                        ytelseType = Ytelse.OMSORGSPENGER,
                        periode = Periode(LocalDate.now(), LocalDate.now().plusDays(10)),
                        gradering = 100,
                    ),
                    SamordningVurderingData(
                        ytelseType = Ytelse.OMSORGSPENGER,
                        periode = Periode(LocalDate.now().plusDays(1), LocalDate.now().plusDays(10)),
                        gradering = 100,
                    )
                )
            )
        }
    }

    @Test
    fun `skal ikke kaste exception når perioder for samme ytelsetype ikke overlapper`() {
        // Should not throw exception
        assertDoesNotThrow({
            VurderingerForSamordning(
                begrunnelse = "...",
                maksDatoEndelig = false,
                fristNyRevurdering = LocalDate.now(),
                vurderteSamordningerData = listOf(
                    SamordningVurderingData(
                        ytelseType = Ytelse.OMSORGSPENGER,
                        periode = Periode(LocalDate.now(), LocalDate.now().plusDays(5)),
                        gradering = 100,
                    ),
                    SamordningVurderingData(
                        ytelseType = Ytelse.OMSORGSPENGER,
                        periode = Periode(LocalDate.now().plusDays(6), LocalDate.now().plusDays(10)),
                        gradering = 100,
                    )
                )
            )
        })
    }

    @Test
    fun `skal ikke kaste exception når perioder for ulike ytelsetyper overlapper`() {
        // Should not throw exception
        assertDoesNotThrow({
            VurderingerForSamordning(
                begrunnelse = "...",
                maksDatoEndelig = false,
                fristNyRevurdering = LocalDate.now(),
                vurderteSamordningerData = listOf(
                    SamordningVurderingData(
                        ytelseType = Ytelse.OMSORGSPENGER,
                        periode = Periode(LocalDate.now(), LocalDate.now().plusDays(10)),
                        gradering = 100,
                    ),
                    SamordningVurderingData(
                        ytelseType = Ytelse.PLEIEPENGER,
                        periode = Periode(LocalDate.now().plusDays(1), LocalDate.now().plusDays(10)),
                        gradering = 100,
                    )
                )
            )
        })
    }
}
