package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov

import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class AvklaringsbehovTest {
    @Test
    fun `kun bestille brev er et helautomatisk avklaringsbehov`() {
        StegType.entries.forEach { stegtype ->
            Definisjon.entries.forEach { definisjon ->
                if (definisjon != Definisjon.BESTILL_BREV) {
                    assertThat(
                        Avklaringsbehov(
                            id = 1,
                            definisjon = definisjon,
                            funnetISteg = stegtype,
                            kreverToTrinn = false
                        ).erAutomatisk()
                    ).isFalse()
                }
            }
        }

        val ab = Avklaringsbehov(
            id = 1,
            definisjon = Definisjon.BESTILL_BREV,
            funnetISteg = StegType.BREV,
            kreverToTrinn = false
        )

        assertThat(ab.erAutomatisk()).isTrue()
    }

    @Test
    fun `Skal ikke kunne gjenåpne et allerede åpnet behov`() {
        val ab = Avklaringsbehov(
            id = 1,
            definisjon = Definisjon.SKRIV_VEDTAKSBREV,
            funnetISteg = StegType.BREV,
            kreverToTrinn = false
        )

        org.junit.jupiter.api.assertThrows<IllegalArgumentException> { ab.reåpne() }
    }
}