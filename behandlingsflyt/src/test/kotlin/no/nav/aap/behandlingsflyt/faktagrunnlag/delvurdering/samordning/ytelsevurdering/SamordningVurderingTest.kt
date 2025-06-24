package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

class SamordningVurderingTest {

    @Test
    fun `lov å ha null i både maksDatoEndelig og maksDato`() {
        assertDoesNotThrow {
            SamordningVurderingGrunnlag(
                begrunnelse = "....",
                maksDatoEndelig = null,
                fristNyRevurdering = null,
                vurderinger = listOf(),
                vurdertAv = "ident"
            )
        }
    }
}