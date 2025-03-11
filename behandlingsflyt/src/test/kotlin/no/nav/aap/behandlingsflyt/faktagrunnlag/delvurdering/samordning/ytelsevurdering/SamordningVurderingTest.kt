package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering

import no.nav.aap.behandlingsflyt.behandling.samordning.Ytelse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

class SamordningVurderingTest {
    @Test
    fun `om maksdato er endelig, må også maksdato oppgis`() {
        assertThrows<IllegalArgumentException> {
            SamordningVurdering(
                ytelseType = Ytelse.SYKEPENGER,
                begrunnelse = "....",
                maksDatoEndelig = true,
                maksDato = null,
                vurderingPerioder = listOf()
            )
        }
    }

    @Test
    fun `om maksdato ikke er endelig kan den være null`() {
        assertDoesNotThrow {
            SamordningVurdering(
                ytelseType = Ytelse.SYKEPENGER,
                begrunnelse = "....",
                maksDatoEndelig = false,
                maksDato = null,
                vurderingPerioder = listOf()
            )
        }
    }
}