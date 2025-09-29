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

public data class Personhendelse(
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
    PERSONGALLERI_V1,
    PERSONGALLERI_PDL_V1,

    AVDOED_PDL_V1,
    GJENLEVENDE_FORELDER_PDL_V1,
    SOEKER_PDL_V1,
    INNSENDER_PDL_V1,

    AVDOED_SOEKNAD_V1,
    SOEKER_SOEKNAD_V1,
    GJENLEVENDE_FORELDER_SOEKNAD_V1,
    INNSENDER_SOEKNAD_V1,

    UTBETALINGSINFORMASJON_V1,
    SOEKNAD_MOTTATT_DATO,
    SAMTYKKE,
    SPRAAK,
    SOEKNADSTYPE_V1,

    NAVN,
    FOEDSELSNUMMER,
    FOEDSELSDATO,
    FOEDSELSAAR,
    FOEDELAND,
    DOEDSDATO,
    ADRESSEBESKYTTELSE,
    BOSTEDSADRESSE,
    DELTBOSTEDSADRESSE,
    KONTAKTADRESSE,
    OPPHOLDSADRESSE,
    SIVILSTATUS,
    SIVILSTAND,
    STATSBORGERSKAP,
    UTLAND,
    FAMILIERELASJON,
    AVDOEDESBARN,
    VERGEMAALELLERFREMTIDSFULLMAKT,
    PERSONROLLE,
    UTENLANDSOPPHOLD,
    UTENLANDSADRESSE,
    SOESKEN_I_BEREGNINGEN,
    HISTORISK_FORELDREANSVAR,

}


public data class Navn(
    val fornavn: String,
    val etternavn: String,
    val mellomnavn: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
public data class PdlHendelseV0(
    val innsedingstype: InnsendingType,
) : PdlHendelse

public fun tilPdlHendelseV0(): PdlHendelse =
    PdlHendelseV0(
        innsedingstype = InnsendingType.PDL_HENDELSE
    )

public fun Personhendelse.tilInnsending(saksnummer: Saksnummer): Innsending =
    Innsending(
        saksnummer = saksnummer,
        referanse = InnsendingReferanse(PdlHendelseId(value = this.hashCode().toString())),
        type = InnsendingType.PDL_HENDELSE,
        kanal = Kanal.DIGITAL,
        mottattTidspunkt = LocalDateTime.now(),
        melding = tilPdlHendelseV0()
    )


