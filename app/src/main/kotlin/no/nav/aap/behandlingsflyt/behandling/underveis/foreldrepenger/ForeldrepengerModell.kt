package no.nav.aap.behandlingsflyt.behandling.underveis.foreldrepenger

import no.nav.aap.verdityper.Periode
import java.time.LocalDate

data class ForeldrepengerRequest (
    val ident: Aktør,
    val periode: Periode,
)

data class ForeldrepengerResponse(
    val ytelser: List<Ytelse>
)

data class Ytelse(
    val ytelse: String,
    val ytelseStatus: String,
    val vedtattTidspunkt: LocalDate,
    val anvist: List<Anvist>
)

data class Anvist(
    val periode: Periode,
    val utbetalingsgrad: Utbetalingsgrad?
)

data class Utbetalingsgrad(
    val verdi: Double
)

data class Aktør(
    val verdi: String
)

/*
data class Ytelse (
    val ytelser: Ytelser,
    val Saksnummer: String,
    val aktør: Aktør
)

enum class Ytelser {
    PLEIEPENGER_SYKT_BARN,
    PLEIEPENGER_NÆRSTÅENDE,
    OMSORGSPENGER,
    OPPLÆRINGSPENGER,
    ENGANGSTØNAD,
    FORELDREPENGER,
    SVANGERSKAPSPENGER,
    FRISINN
}*/
