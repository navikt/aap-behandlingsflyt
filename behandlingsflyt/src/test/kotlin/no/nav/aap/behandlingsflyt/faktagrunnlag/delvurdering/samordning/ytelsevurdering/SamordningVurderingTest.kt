package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

class SamordningVurderingTest {
    @Test
    fun `om maksdato er endelig, må også maksdato oppgis`() {
        assertThrows<IllegalArgumentException> {
            SamordningVurderingGrunnlag(
                begrunnelse = "....",
                maksDatoEndelig = true,
                maksDato = null,
                vurderinger = listOf()
            )
        }
    }

    @Test
    fun `om maksdato ikke er endelig kan den være null`() {
        assertDoesNotThrow {
            SamordningVurderingGrunnlag(
                begrunnelse = "....",
                maksDatoEndelig = false,
                maksDato = null,
                vurderinger = listOf()
            )
        }
    }
}