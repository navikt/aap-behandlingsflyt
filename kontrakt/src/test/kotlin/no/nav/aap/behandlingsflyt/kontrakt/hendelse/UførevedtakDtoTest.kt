package no.nav.aap.behandlingsflyt.kontrakt.hendelse

import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Uførevedtak
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.UførevedtakKilde
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.UførevedtakResultat
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.UførevedtakV0
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.UførevedtakV1
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class UførevedtakDtoTest {
    @Test
    fun `begge konverteringene for V0 gir samme dto`() {
        val dato = LocalDate.of(2026, 10, 10)
        val v0 = UførevedtakV0("12345678901", dato, UførevedtakResultat.INNV, false, UførevedtakKilde.PENSJON)
        val vedtak: Uførevedtak = v0
        val forventet = UførevedtakDto(UførevedtakResultatDto.INNVILGELSE, dato)

        assertThat(v0.tilUføreVedtakDto()).isEqualTo(forventet)
        assertThat(vedtak.tilUføreVedtakDto()).isEqualTo(forventet)
    }

    @Test
    fun `V1 konverteres til dto`() {
        val dato = LocalDate.of(2026, 10, 10)
        val vedtak: Uførevedtak =
            UførevedtakV1("12345678901", dato, UførevedtakResultat.AVSL, false, UførevedtakKilde.PENSJON, dato)

        assertThat(vedtak.tilUføreVedtakDto()).isEqualTo(UførevedtakDto(UførevedtakResultatDto.AVSLAG, dato))
    }
}
