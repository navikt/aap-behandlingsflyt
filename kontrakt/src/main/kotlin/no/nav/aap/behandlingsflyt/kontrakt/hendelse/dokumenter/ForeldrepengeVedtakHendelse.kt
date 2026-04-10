package no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.ForeldrepengevedtakReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.verdityper.dokument.Kanal
import java.time.LocalDateTime
import java.time.OffsetDateTime

public sealed interface ForeldrepengeVedtak : Melding

@JsonIgnoreProperties(ignoreUnknown = true)
public data class ForeldrepengevedtakKafkaMelding(
    val personidentifikator: String,
    val tidspunkt: OffsetDateTime,
    val tema: String, // FOR, OMS, FRI
)
{
    public fun tilForeldrepengeVedtakV0(): ForeldrepengeVedtak =
        ForeldrepengeVedtakV0(
            personidentifikator = personidentifikator,
            tidspunkt = tidspunkt,
            tema = tema,
            kilde = ForeldrepengeVedtakKilde.FP_SAK
        )

    public fun tilInnsending(meldingKey: String, saksnummer: Saksnummer): Innsending {
        return Innsending(
            saksnummer = saksnummer,
            referanse = InnsendingReferanse(ForeldrepengevedtakReferanse.ny("${meldingKey}_${tidspunkt.toLocalDate()}")),
            type = InnsendingType.FORELDREPENGE_VEDTAK_HENDELSE,
            kanal = Kanal.DIGITAL,
            mottattTidspunkt = LocalDateTime.now(),
            melding = this.tilForeldrepengeVedtakV0()
        )
    }

}

@JsonIgnoreProperties(ignoreUnknown = true)
public data class ForeldrepengeVedtakV0(
    val personidentifikator: String,
    val tidspunkt: OffsetDateTime,
    val tema: String,
    val kilde: ForeldrepengeVedtakKilde,
) : ForeldrepengeVedtak

public enum class ForeldrepengeVedtakKilde {
    FP_SAK
}

