package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

class SamordningVurderingTest {

    @Test
    fun `lov å ha null i både maksDatoEndelig og maksDato`() {
        assertDoesNotThrow {
            SamordningVurderingGrunnlag(
                begrunnelse = "....",
                maksDatoEndelig = null,
                maksDato = null,
                vurderinger = listOf()
            )
        }
    }
}