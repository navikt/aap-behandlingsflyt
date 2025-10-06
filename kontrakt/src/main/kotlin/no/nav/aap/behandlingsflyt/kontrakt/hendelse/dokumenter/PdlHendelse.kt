package no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter

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
    ADRESSEBESKYTTELSE_V1,
    DOEDFOEDT_BARN_V1,
    DOEDSFALL_V1,
    FALSK_ID_V1,
    FAMILIERELASJON_V1,
    FOEDSEL_V1,
    FOEDSELSDATO_V1,
    FORELDERBARNRELASJON_V1,
    SIVILSTAND_V1,
    VERGEMAAL_ELLER_FREMTIDSFULLMAKT_V1,
    UTFLYTTING_FRA_NORGE,
    INNFLYTTING_TIL_NORGE,
    FOLKEREGISTERIDENTIFIKATOR_V1,
    NAVN_V1,
    SIKKERHETSTILTAK_V1,
    STATSBORGERSKAP_V1,
    TELEFONNUMMER_V1,
    KONTAKTADRESSE_V1,
    BOSTEDSADRESSE_V1,
    OPPHOLDSADRESSE_V1
}


public data class Navn(
    val fornavn: String, val etternavn: String, val mellomnavn: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
public data class PdlHendelseV0(
    val innsendingstype: InnsendingType,
) : PdlHendelse

public fun tilPdlHendelseV0(): PdlHendelse = PdlHendelseV0(
    innsendingstype = InnsendingType.PDL_HENDELSE
)

public fun PdlPersonHendelse.tilInnsending(saksnummer: Saksnummer): Innsending = Innsending(
    saksnummer = saksnummer,
    referanse = InnsendingReferanse(PdlHendelseId(value = this.hendelseId)),
    type = InnsendingType.PDL_HENDELSE,
    kanal = Kanal.DIGITAL,
    mottattTidspunkt = LocalDateTime.now(),
    melding = tilPdlHendelseV0()
)


