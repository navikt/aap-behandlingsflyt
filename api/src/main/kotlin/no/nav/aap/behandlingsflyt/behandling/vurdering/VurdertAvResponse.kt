package no.nav.aap.behandlingsflyt.behandling.vurdering

import java.time.LocalDate

data class VurdertAvResponse(
    val ident: String,
    val dato: LocalDate
)