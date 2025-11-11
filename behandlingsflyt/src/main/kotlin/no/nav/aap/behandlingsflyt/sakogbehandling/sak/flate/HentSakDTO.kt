package no.nav.aap.behandlingsflyt.sakogbehandling.sak.flate

import com.papsign.ktor.openapigen.annotations.parameters.PathParam

data class HentSakDTO(@param:PathParam("saksnummer") val saksnummer: String)

data class HentAntallSakerDTO(@param:PathParam("antall") val antall: Int)
