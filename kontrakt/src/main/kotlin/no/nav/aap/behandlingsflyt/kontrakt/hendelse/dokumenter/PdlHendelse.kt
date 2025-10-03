package no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.PdlHendelseId
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.verdityper.dokument.Kanal
import java.time.Instant
import java.time.LocalDateTime


public sealed interface PdlHendelse : Melding

public data class PdlPersonHendelse(
    val hendelseId: String,
    val personidenter: List<String>,
    val master: String,
    val opprettet: Instant,
    val opplysningstype: Opplysningstype,
    val endringstype: Endringstype,
    val tidligereHendelseId: String? = null,
    val navn: Navn? = null
)

public enum class Endringstype {
    OPPRETTET, KORRIGERT, ANNULLERT, OPPHOERT
}

public enum class Opplysningstype {

    @JsonEnumDefaultValue
    UNKNOWN,

    PERSONGALLERI_V1,
    PERSONGALLERI_PDL_V1,

    AVDOED_PDL_V1,
    GJENLEVENDE_FORELDER_PDL_V1,
    SOEKER_PDL_V1,
    INNSENDER_PDL_V1,

}


public data class Navn(
    val fornavn: String,
    val etternavn: String,
    val mellomnavn: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
public data class PdlHendelseV0(
    val innsendingstype: InnsendingType,
) : PdlHendelse

public fun tilPdlHendelseV0(): PdlHendelse =
    PdlHendelseV0(
        innsendingstype = InnsendingType.PDL_HENDELSE
    )

public fun PdlPersonHendelse.tilInnsending(saksnummer: Saksnummer): Innsending =
    Innsending(
        saksnummer = saksnummer,
        referanse = InnsendingReferanse(PdlHendelseId(value = this.hendelseId)),
        type = InnsendingType.PDL_HENDELSE,
        kanal = Kanal.DIGITAL,
        mottattTidspunkt = LocalDateTime.now(),
        melding = tilPdlHendelseV0()
    )


