package no.nav.aap.behandlingsflyt.sak.flate

import com.papsign.ktor.openapigen.annotations.parameters.PathParam

class HentSakDTO(@PathParam("saksnummer") val saksnummer: String)
