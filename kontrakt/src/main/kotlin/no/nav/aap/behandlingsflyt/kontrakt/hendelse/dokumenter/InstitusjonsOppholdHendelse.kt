package no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InstitusjonsOppholdHendelseId
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.verdityper.dokument.Kanal
import java.time.LocalDate
import java.time.LocalDateTime

public sealed interface InstitusjonsOppholdHendelse : Melding

@JsonIgnoreProperties(ignoreUnknown = true)
public data class InstitusjonsOppholdHendelseKafkaMelding(
    val hendelseId: Long,
    var oppholdId: Long,
    var norskident: String,
    var type: InstitusjonsoppholdsType,
    var kilde: InstitusjonsoppholdKilde,
    var institusjonsOpphold: Inst2KafkaDto?
)
{
    public fun tilInstitusjonsOppholdHendelseV0(): InstitusjonsOppholdHendelse =
        InstitusjonsOppholdHendelseV0(
            hendelsesid = hendelseId,
            eksternFagsakId = oppholdId,
            norskIdent = norskident,
            institusjonsOpphold = institusjonsOpphold
        )

    public fun tilInnsending(meldingKey: String, saksnummer: Saksnummer): Innsending {
        return Innsending(
            saksnummer = saksnummer,
            referanse = InnsendingReferanse(InstitusjonsOppholdHendelseId.ny()),
            type = InnsendingType.INSTITUSJONSOPPHOLD,
            kanal = Kanal.DIGITAL,
            mottattTidspunkt = LocalDateTime.now(),
            melding = this.tilInstitusjonsOppholdHendelseV0()
        )
    }

}

@JsonIgnoreProperties(ignoreUnknown = true)
public data class InstitusjonsOppholdHendelseV0(
    val hendelsesid: Long,
    val eksternFagsakId: Long,
    val norskIdent: String,
    val institusjonsOpphold: Inst2KafkaDto?,
) : InstitusjonsOppholdHendelse

public data class Inst2KafkaDto(
    val startdato: LocalDate,
    val sluttdato: LocalDate?,
)

public enum class InstitusjonsoppholdsType {
    INNMELDING,
    OPPDATERING,
    UTMELDING,
    ANNULERING,
}

public enum class InstitusjonsoppholdKilde {
    APPBRK,
    KDI,
    IT,
    INST,
}

