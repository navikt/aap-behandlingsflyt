package no.nav.aap.behandlingsflyt.hendelse.datadeling

import java.time.LocalDate


data class ArenaLovvalgOgMedlemskapResponse(
    val sakId: String,
    val medlemskapTom: LocalDate?, // TODO finne ut hva som trengs her
)