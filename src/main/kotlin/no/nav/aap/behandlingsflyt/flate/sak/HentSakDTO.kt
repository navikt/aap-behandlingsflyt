package no.nav.aap.behandlingsflyt.flate.sak

import com.papsign.ktor.openapigen.annotations.parameters.PathParam

class HentSakDTO(@PathParam("saksnummer") val saksnummer: String) {

}
