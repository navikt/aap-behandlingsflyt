package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.meldeperiode

import no.nav.aap.behandlingsflyt.test.juli
import no.nav.aap.behandlingsflyt.test.juni
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class MeldeperiodeUtlederTest {
    @Test
    fun `første meldeperiode er siste fastsatt dag pluss n x 14 dager før før aktuellTidsperiode`() {
        val res = MeldeperiodeUtleder.utledMeldeperiode(22 juni 2026, Periode(23 juni 2026, 24 juli 2026))

        assertThat(res.first().fom).isEqualTo(22 juni 2026)

        val res2 = MeldeperiodeUtleder.utledMeldeperiode(15 juni 2026, Periode(23 juni 2026, 24 juli 2026))

        assertThat(res2.first().fom).isEqualTo(15 juni 2026)
    }
}