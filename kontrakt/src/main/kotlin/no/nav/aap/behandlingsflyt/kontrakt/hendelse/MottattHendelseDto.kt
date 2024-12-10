package no.nav.aap.behandlingsflyt.kontrakt.hendelse

import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.Melding.Søknad
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.verdityper.dokument.JournalpostId
import no.nav.aap.verdityper.dokument.Kanal
import java.time.LocalDate

public data class MottattHendelseDto(
    val saksnummer: Saksnummer,
    val type: InnsendingType,
    val kanal: Kanal,
    val hendelseId: InnsendingReferanse,
    val payload: Any?
)

public class Innsending(
    public val saksnummer: Saksnummer,
    public val referanse: InnsendingReferanse,
    public val type: InnsendingType,
    public val melding: Melding,
)


///**
// * @sample no.nav.aap.behandlingsflyt.kontrakt.hendelse.example
// */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "meldingType", visible = true)
public interface Melding {
    public sealed interface Søknad : Melding
    public sealed interface Pliktkort : Melding
    public sealed interface Dialogmelding : Melding
    public sealed interface Legeerklærling : Melding
    public sealed interface LegeerklærlingAvvist : Melding
    public sealed interface DialogMelding : Melding
}


// SØKNAD

/**
 * Dagens søknad-objekt. Ved inkompatibel endring, lag ny versjon.
 */
public data class SøknadV0(
    public val student: SøknadStudentDto,
    public val yrkesskade: String,
    public val oppgitteBarn: OppgitteBarn?
) : Søknad

public data class SøknadV1(
    public val student: SøknadStudentDto,
    public val yrkesskade: String,
    public val oppgitteBarn: OppgitteBarn?,
    public val nyttFelt: Number
) : Søknad

public class SøknadStudentDto(
    public val erStudent: String,
    public val kommeTilbake: String? = null
)

public class OppgitteBarn(public val identer: Set<String>)

// PLIKTKORT

public class PliktkortV0(public val jfnr: Double, type: InnsendingType) : Melding.Pliktkort

// LEGEERKLÆRING

public class LegeerklæringV0(
    public val journalpostId: String
) : Melding.Legeerklærling

// DIALOGMELDING

public class DialogmeldingV0(public val fnr: Number) : Melding.Dialogmelding


/**
 * Eksempel på hvordan håndtere meldingtyper.
 */
private fun example(innsending: Innsending) {
    when (innsending.melding) {
        is Melding.Søknad -> when (innsending.melding) {
            is SøknadV0 -> TODO()
            is SøknadV1 -> TODO()
        }

        is Melding.Pliktkort -> when (innsending.melding) {
            is PliktkortV0 -> TODO()
        }

        is Melding.Legeerklærling -> when (innsending.melding) {
            is LegeerklæringV0 -> TODO()
        }

        is Melding.Dialogmelding -> TODO()
    }
}


public data class PeriodeMedArbeid(
    val fom: LocalDate,
    val tom: LocalDate?,
    public val timerIArbeid: Double
)
