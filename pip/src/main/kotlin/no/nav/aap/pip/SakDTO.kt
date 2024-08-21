package no.nav.aap.pip

import com.papsign.ktor.openapigen.annotations.parameters.PathParam

data class SakDTO(@PathParam("saksnummer") val saksnummer: String)
