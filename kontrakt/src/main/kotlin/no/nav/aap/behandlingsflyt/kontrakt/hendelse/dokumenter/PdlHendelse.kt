package no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.LocalDate

public open class PdlHendelse()

@JsonIgnoreProperties(ignoreUnknown = true)
public data class PdlHendelseKafkaMelding(
    val personId: String,
    val doedsdato: LocalDate
) {

    public fun tilPersonHendelse(): Doedsfall =
        Doedsfall(
            ident = personId,
            doedsdato = doedsdato,
        )
}

@JsonIgnoreProperties(ignoreUnknown = true)
public data class Doedsfall(
    val ident: String,
    val doedsdato: LocalDate,
) : PdlHendelse()



