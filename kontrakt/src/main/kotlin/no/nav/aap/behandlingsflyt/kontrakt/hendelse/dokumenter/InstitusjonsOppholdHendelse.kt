package no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.TilbakekrevingHendelseId
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.verdityper.dokument.Kanal
import java.time.LocalDate
import java.time.LocalDateTime

public sealed interface InstitusjonsOppholdHendelse : Melding

@JsonIgnoreProperties(ignoreUnknown = true)
public data class Inst2HendelseKafkaMelding(
    val hendelseId: Long,
    var oppholdId: Long,
    var norskident: String,
    var type: InstitusjonsoppholdsType,
    var kilde: InstitusjonsoppholdKilde,
    var institusjonsOpphold: Inst2KafkaDto
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
            referanse = InnsendingReferanse(TilbakekrevingHendelseId.ny(meldingKey)),
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
    val institusjonsOpphold: Inst2KafkaDto,
) : InstitusjonsOppholdHendelse

public data class Inst2KafkaDto(
    val oppholdId: Long,
    val tssEksternId: String,
    val institusjonsnavn: String? = null,
    val avdelingsnavn: String? = null,
    val organisasjonsnummer: String? = null,
    val institusjonstype: String? = null,
    val varighet: String? = null,
    val kategori: String? = null,
    val startdato: LocalDate,
    val faktiskSluttdato: LocalDate? = null,
    val forventetSluttdato: LocalDate? = null,
    val kilde: String? = null,
    val overfoert: Boolean? = null,
    val registrertAv: String? = null,
    val endretAv: String? = null,
    val endringstidspunkt: LocalDateTime? = null,
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
