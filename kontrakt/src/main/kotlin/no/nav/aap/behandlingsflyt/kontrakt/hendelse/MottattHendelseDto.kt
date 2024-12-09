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

public sealed interface Innsending {
    public val saksnummer: Saksnummer
    public val type: InnsendingType
    public val referanse: InnsendingReferanse
}

public class Pliktkort(
    override val referanse: InnsendingReferanse,
    override val saksnummer: Saksnummer,
    override val type: InnsendingType,
    public val timerArbeidet: List<PeriodeMedArbeid>
) : Innsending

public data class PeriodeMedArbeid(
    val fom: LocalDate,
    val tom: LocalDate?,
    public val timerIArbeid: Double
)

public class Søknad(
    override val referanse: InnsendingReferanse,
    override val saksnummer: Saksnummer,
    override val type: InnsendingType,
    public val søknad: Any // Søknad-felter
) : Innsending

public class Legeærklaring(
    override val referanse: InnsendingReferanse,
    override val saksnummer: Saksnummer,
    override val type: InnsendingType,
    public val ff: String
) : Innsending