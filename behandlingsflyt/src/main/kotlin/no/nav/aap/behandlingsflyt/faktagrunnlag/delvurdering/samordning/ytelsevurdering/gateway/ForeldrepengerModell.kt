package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway

import no.nav.aap.komponenter.type.Periode
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
    val saksnummer: Long,
    val kildesystem: String,
    val ytelseStatus: String,
    val vedtattTidspunkt: LocalDate,
    val anvist: List<Anvist>
)

data class Anvist(
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