package no.nav.aap.behandlingsflyt.hendelse.mottak

import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.UførevedtakKilde
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.UførevedtakResultat
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.UførevedtakV0
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.UførevedtakV1
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.UførevedtakKafkaMelding
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.UførevedtakKafkaMeldingV1
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Melding
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.komponenter.json.DefaultJsonMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
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

    @Test
    fun `opphørsdato er dagen uførevedtaket ble fattet`() {
        val vedtaksdato = LocalDate.of(2026, 10, 10)
        val vedtak = UførevedtakV1(
            personId = "12345678901",
            virkningsdato = vedtaksdato.minusMonths(1),
            resultat = UførevedtakResultat.INNV,
            avslag12_5 = false,
            kilde = UførevedtakKilde.PENSJON,
            vedtaksdato = vedtaksdato
        )

        assertThat(vedtak.opphørsdato(vedtaksdato.plusDays(5))).isEqualTo(vedtaksdato)
    }

    @ParameterizedTest
    @CsvSource("0", "1", "10")
    fun `virkningsdato på mottaksdagen eller senere brukes fremfor vedtaksdato`(dagerTilVirkningsdato: Long) {
        val mottattDato = LocalDate.of(2026, 10, 21)
        val vedtak = UførevedtakV1(
            personId = "12345678901",
            virkningsdato = mottattDato.plusDays(dagerTilVirkningsdato),
            resultat = UførevedtakResultat.INNV,
            avslag12_5 = false,
            kilde = UførevedtakKilde.PENSJON,
            vedtaksdato = mottattDato.minusDays(1)
        )

        assertThat(vedtak.opphørsdato(mottattDato)).isEqualTo(vedtak.virkningsdato)
    }

    @Test
    fun `vedtaksdato bevares når meldingen lagres og leses`() {
        val dato = LocalDate.of(2026, 10, 10)
        val json = """
            {"personId":"12345678901","virkningsdato":"2026-09-10","resultat":"INNV",
             "avslag12_5":false,"vedtaksdato":"2026-10-10"}
        """.trimIndent()
        val kafkaMelding = DefaultJsonMapper.fromJson<UførevedtakKafkaMeldingV1>(json)
        val innsendtMelding = requireNotNull(kafkaMelding.tilInnsending("0-1", Saksnummer("TEST_1")).melding)
        val lagret = DefaultJsonMapper.toJson(innsendtMelding)

        assertThat(DefaultJsonMapper.fromJson<Melding>(lagret))
            .isEqualTo(innsendtMelding)
        assertThat((innsendtMelding as UførevedtakV1).vedtaksdato).isEqualTo(dato)
    }

    @Test
    fun `eldre kafka-melding uten vedtaksdato kan fortsatt leses`() {
        val gammel = UførevedtakKafkaMelding(
            "12345678901", LocalDate.of(2026, 9, 10), UførevedtakResultat.INNV, false
        )
        val json = DefaultJsonMapper.toJson(gammel)
        val melding = DefaultJsonMapper.fromJson<UførevedtakKafkaMeldingV1>(json)

        assertThat(melding.tilInnsending("0-1", Saksnummer("TEST_1")).melding).isInstanceOf(UførevedtakV0::class.java)
    }
}
