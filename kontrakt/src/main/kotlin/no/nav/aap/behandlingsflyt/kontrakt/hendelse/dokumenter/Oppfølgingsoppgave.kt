package no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.tilgang.Rolle
import java.time.LocalDate

public sealed interface Oppfølgingsoppgave : Melding

/**
 * Hvem som skal tildeles oppfølgingsbehandlingen.
 */
public enum class HvemSkalFølgeOpp {
    NasjonalEnhet,
    Lokalkontor
}
public data class Opprinnelse(val behandlingsreferanse: String?,
                              val avklaringsbehovKode: String?
)


/**
 * @param hvemSkalFølgeOpp Ident til bruker som skal følge opp
 * @param reserverTilBruker Hvis oppgitt, så skal oppgave-appen automatisk reservere oppfølgingsoppgaven til denne brukeren.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public data class OppfølgingsoppgaveV0(
    public val datoForOppfølging: LocalDate,
    public val hvemSkalFølgeOpp: HvemSkalFølgeOpp,
    public val reserverTilBruker: String?,
    public val hvaSkalFølgesOpp: String,
    public val opprinnelse: Opprinnelse?
) : Oppfølgingsoppgave
