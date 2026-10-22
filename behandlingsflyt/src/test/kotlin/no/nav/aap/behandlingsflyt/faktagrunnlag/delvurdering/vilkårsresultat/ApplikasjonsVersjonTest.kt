package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ApplikasjonsVersjonTest {
    @Test
    fun `bruker stabil utviklingsversjon uten produksjonspakking`() {
        assertThat(ApplikasjonsVersjon.versjon).isEqualTo("development")
    }
}
