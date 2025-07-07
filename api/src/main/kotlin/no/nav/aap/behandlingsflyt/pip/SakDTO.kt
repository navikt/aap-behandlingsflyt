package no.nav.aap.behandlingsflyt.pip

import com.papsign.ktor.openapigen.annotations.parameters.PathParam

data class SakDTO(@param:PathParam("saksnummer") val saksnummer: String)
