package no.nav.aap.behandlingsflyt.dbtest

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class IdentTest {

    @Test
    fun hentIdent() {
        val førsteIdent = Ident.hentNesteIdent()
        val andreIdent = Ident.hentNesteIdent()
        assertThat(førsteIdent).isLessThan(andreIdent)
    }
}
