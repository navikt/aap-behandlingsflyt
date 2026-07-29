package no.nav.aap.behandlingsflyt.kontrakt.ytelseoppslag

import java.time.LocalDate

/**
 * Felles request for oppslag av utbetalte ytelsesperioder på en person.
 *
 * Endepunktene returnerer rådata for perioder som overlapper oppslagsvinduet - konsumenten
 * bestemmer selv vinduet, og avgjør selv hvilke perioder som er relevante.
 *
 * @param personident Identen det skal slås opp på. Historiske identer slås opp automatisk.
 * @param fom Start på oppslagsvinduet.
 * @param tom Slutt på oppslagsvinduet.
 */
public data class YtelseoppslagRequest(
    val personident: String,
    val fom: LocalDate,
    val tom: LocalDate,
)

public data class YtelseoppslagPeriodeDTO(
    val fom: LocalDate,
    val tom: LocalDate,
)


