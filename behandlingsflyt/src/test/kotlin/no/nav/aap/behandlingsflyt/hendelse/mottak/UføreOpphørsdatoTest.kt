package no.nav.aap.behandlingsflyt.hendelse.mottak

import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.UførevedtakKilde
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.UførevedtakResultat
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.UførevedtakV0
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.time.LocalDate

class UføreOpphørsdatoTest {
    @ParameterizedTest
    @CsvSource("-10, 1", "-1, 1", "0, 0", "1, 1", "10, 10")
    fun `opphørsdato beregnes ut fra virkningsdato og mottaksdato`(
        dagerTilVirkningsdato: Long,
        dagerTilOpphør: Long,
    ) {
        val mottattDato = LocalDate.of(2026, 10, 21)
        val vedtak = UførevedtakV0(
            personId = "12345678901",
            virkningsdato = mottattDato.plusDays(dagerTilVirkningsdato),
            resultat = UførevedtakResultat.INNV,
            avslag12_5 = false,
            kilde = UførevedtakKilde.PENSJON,
        )

        assertThat(vedtak.opphørsdato(mottattDato)).isEqualTo(mottattDato.plusDays(dagerTilOpphør))
    }
}
