package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.stønadsperiode

import java.time.LocalDate

/**
 * Arena-saken som stønadsperioden er kant i kant med.
 */
data class KantIKantMedArena(
    val arenaSaksnummer: String,
    val status: String,
    val maksdato: LocalDate,
    val vedtakTil: LocalDate?,
    val vedtakId: Int,
)
