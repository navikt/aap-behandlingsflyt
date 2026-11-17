package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.GjenståendeSykedager
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.GjenståendeSykedagerGrunnlag
import no.nav.aap.behandlingsflyt.test.januar
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class GjenståendeSykedagerTest {
    @Test
    fun `utleder maksdato ved å legge gjenstående hverdager til vurdertTilOgMed`() {
        val sykedager = GjenståendeSykedager(GjenståendeSykedagerGrunnlag.VEDTAK_I_NY_LØSNING, 5, 1 januar 2026)

        assertThat(sykedager.utledMaksdato()).isEqualTo(8 januar 2026)
    }

    @Test
    fun `utleder ikke maksdato når grunnlaget ikke er vedtak i ny løsning`() {
        val sykedager = GjenståendeSykedager(GjenståendeSykedagerGrunnlag.MAKSDATO_I_INFOTRYGD, 5, 1 januar 2026)

        assertThat(sykedager.utledMaksdato()).isNull()
    }

    @Test
    fun `utleder ikke maksdato når dager eller dato mangler`() {
        assertThat(
            GjenståendeSykedager(GjenståendeSykedagerGrunnlag.VEDTAK_I_NY_LØSNING, null, 1 januar 2026).utledMaksdato()
        ).isNull()
        assertThat(
            GjenståendeSykedager(GjenståendeSykedagerGrunnlag.VEDTAK_I_NY_LØSNING, 5, null).utledMaksdato()
        ).isNull()
    }
}
