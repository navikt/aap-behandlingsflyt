package no.nav.aap.behandlingsflyt.behandling.maksdato

import java.time.LocalDate

enum class MaksdatoKilde { ARENA, KELVIN }

/** Kildeuavhengig maksdato per sak. */
data class Maksdato(
    val kilde: MaksdatoKilde,
    val saksnummer: String,
    val status: String?,
    val maksdato: LocalDate?,
    val vedtakTil: LocalDate?,
)
