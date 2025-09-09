package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway

import no.nav.aap.komponenter.type.Periode
import java.time.LocalDate

data class ForeldrepengerRequest(
    val ident: Aktør,
    val periode: Periode,
)

data class ForeldrepengerResponse(
    val ytelser: List<Ytelse>
)

/**
 * [Kilde for denne](https://github.com/navikt/fp-abakus/blob/master/kontrakt-vedtak/src/main/java/no/nav/abakus/vedtak/ytelse/Ytelser.java#L3)
 * og [denne](https://github.com/navikt/fp-abakus/blob/master/kontrakt-vedtak/src/main/java/no/nav/abakus/vedtak/ytelse/v1/YtelseV1.java)
 *
 * @param ytelseStatus Mulige verdier: UNDER_BEHANDLING_LØPENDE,AVSLUTTET,UKJENT.
 * @param kildesystem Mulige verdier: FPSAK og K9SAK
 */
data class Ytelse(
    val ytelse: Ytelser,
    val saksnummer: String,
    val kildesystem: String,
    val ytelseStatus: String,
    val vedtattTidspunkt: LocalDate,
    val anvist: List<Anvist>
)

data class Anvist(
    // Hva betyr denne? hva om det er en aktiv ytelse?
    // Kan slutt-dato *ikke* være satt?
    val periode: Periode,
    val utbetalingsgrad: Utbetalingsgrad,
    val beløp: Number?
)

data class Utbetalingsgrad(
    val verdi: Number
)

data class Aktør(
    val verdi: String
)

// Kopiert herfra: https://github.com/navikt/fp-abakus/blob/master/kontrakt-vedtak/src/main/java/no/nav/abakus/vedtak/ytelse/Ytelser.java#L3
enum class Ytelser {
    /**
     * Folketrygdloven K9 ytelser.
     */
    PLEIEPENGER_SYKT_BARN,
    PLEIEPENGER_NÆRSTÅENDE,
    OMSORGSPENGER,
    OPPLÆRINGSPENGER,

    /**
     * Folketrygdloven K14 ytelser.
     */
    ENGANGSTØNAD,
    FORELDREPENGER,
    SVANGERSKAPSPENGER
}
