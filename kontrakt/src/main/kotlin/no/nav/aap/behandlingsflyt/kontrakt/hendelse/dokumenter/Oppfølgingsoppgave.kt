package no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.time.LocalDate

public sealed interface Oppfølgingsoppgave : Melding

/**
 * Hvem som skal tildeles oppfølgingsbehandlingen.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
@JsonSubTypes(
    JsonSubTypes.Type(HvemSkalFølgeOpp.Bruker::class, name = "bruker"),
    JsonSubTypes.Type(HvemSkalFølgeOpp.Kontor::class, name = "kontor"),
    JsonSubTypes.Type(HvemSkalFølgeOpp.NasjonalEnhet::class, name = "nasjonalEnhet"),
)
public sealed class HvemSkalFølgeOpp {
    public data class Bruker(public val ident: String) : HvemSkalFølgeOpp()
    public data class Kontor(public val kode: String) : HvemSkalFølgeOpp()
    public class NasjonalEnhet : HvemSkalFølgeOpp() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            return true
        }

        override fun hashCode(): Int {
            return javaClass.hashCode()
        }
    }
}

/**
 * @param hvemFølgerOpp Ident til bruker som skal følge opp
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public data class OppfølgingsoppgaveV0(
    public val datoForOppfølging: LocalDate,
    public val hvemSkalFølgeOpp: HvemSkalFølgeOpp,
    public val hvaSkalFølgesOpp: String
) : Oppfølgingsoppgave
