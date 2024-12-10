package no.nav.aap.behandlingsflyt.kontrakt.hendelse

import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.verdityper.dokument.Kanal
import java.time.LocalDate

public data class MottattHendelseDto(
    // ha denne i url i stedet?
    val saksnummer: Saksnummer,
    val type: InnsendingType,
    val kanal: Kanal,
    val hendelseId: InnsendingReferanse,
    val payload: Any?
)

public class Innsending(
    public val saksnummer: Saksnummer,
    public val type: InnsendingType,
    public val referanse: InnsendingReferanse,
    public val melding: Melding
)

public sealed interface Melding

public sealed interface Pliktkort : Melding

public class PliktkortV0(public val jfnr: Double) : Pliktkort

public sealed interface Søknad : Melding

public class SøknadV0(
    public val student: SøknadStudentDto,
    public val yrkesskade: String,
    public val oppgitteBarn: OppgitteBarn?
) : Søknad

public class SøknadStudentDto(
    public val erStudent: String,
    public val kommeTilbake: String? = null
)

public class SøknadV1(
    public val student: SøknadStudentDto,
    public val yrkesskade: String,
    public val oppgitteBarn: OppgitteBarn?,
    public val nyttFelt: Number
) : Søknad

public class OppgitteBarn(public val identer: Set<String>)

public sealed interface Legeerklærling : Melding

public class LegeerklæringV0(public val fnr: Number) : Legeerklærling


fun example(innsending: Innsending) {
    when (innsending.melding) {
        is Søknad -> when (innsending.melding) {
            is SøknadV0 -> TODO()
            is SøknadV1 -> TODO()
        }

        is Pliktkort -> when (innsending.melding) {
            is PliktkortV0 -> TODO()
        }

        is Legeerklærling -> when (innsending.melding) {
            is LegeerklæringV0 -> TODO()
        }
    }
}


//@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "behovstype", visible = true)
//public sealed interface Innsending {
//    public val saksnummer: Saksnummer
//    public val type: InnsendingType
//    public val referanse: InnsendingReferanse
//}

//public class Pliktkort(
//    override val referanse: InnsendingReferanse,
//    override val saksnummer: Saksnummer,
//    override val type: InnsendingType,
//    public val timerArbeidet: List<PeriodeMedArbeid>
//) : Innsending

public data class PeriodeMedArbeid(
    val fom: LocalDate,
    val tom: LocalDate?,
    public val timerIArbeid: Double
)

//public class Søknad(
//    override val referanse: InnsendingReferanse,
//    override val saksnummer: Saksnummer,
//    override val type: InnsendingType,
//    public val søknad: Any // Søknad-felter
//) : Innsending
//
//public class Legeærklaring(
//    override val referanse: InnsendingReferanse,
//    override val saksnummer: Saksnummer,
//    override val type: InnsendingType,
//    public val ff: String
//) : Innsending