package no.nav.aap.behandlingsflyt.kontrakt.hendelse

import java.time.LocalDate

/**
 * Sendes for førstegangsbehandlinger der personen har en Arena-sak med maksdato.
 *
 * @param erKantIKant Et krav i saken dekker overgangen fra Arena, altså dagen etter maksdato til og med første
 * hverdag etter maksdato.
 * @param maksdato Maksdato i Arena-saken.
 * @param kravdato Mulig rett fra for kravet som er kant i kant, ellers for det tidligste søknadskravet.
 */
public data class NySakEtterOpphørArenaDto(
    val erKantIKant: Boolean,
    val maksdato: LocalDate,
    val kravdato: LocalDate
)
