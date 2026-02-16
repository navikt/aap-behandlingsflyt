package no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.SykepengevedtakReferanse
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.verdityper.dokument.Kanal
import java.time.LocalDateTime
import java.time.OffsetDateTime

public sealed interface SykepengeSpleisVedtak : Melding

@JsonIgnoreProperties(ignoreUnknown = true)
public data class SykepengevedtakKafkaMelding(
    val personidentifikator: String,
    val tidspunkt: OffsetDateTime,
)
{
    public fun tilSykepengeSpleisVedtakV0(): SykepengeSpleisVedtak =
        SykepengevedtakV0(
            personidentifikator = personidentifikator,
            tidspunkt = tidspunkt,
            kilde = SykepengevedtakKilde.SPLEIS
        )

    public fun tilInnsending(meldingKey: String, saksnummer: Saksnummer): Innsending {
        return Innsending(
            saksnummer = saksnummer,
            referanse = InnsendingReferanse(SykepengevedtakReferanse.ny("${meldingKey}_${tidspunkt}")),
            type = InnsendingType.SYKEPENGE_VEDTAK_HENDELSE,
            kanal = Kanal.DIGITAL,
            mottattTidspunkt = LocalDateTime.now(),
            melding = this.tilSykepengeSpleisVedtakV0()
        )
    }

}

@JsonIgnoreProperties(ignoreUnknown = true)
public data class SykepengevedtakV0(
    val personidentifikator: String,
    val tidspunkt: OffsetDateTime,
    val kilde: SykepengevedtakKilde,
) : SykepengeSpleisVedtak

public enum class SykepengevedtakKilde {
    SPLEIS,
}

