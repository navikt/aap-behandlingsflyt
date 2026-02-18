package no.nav.aap.behandlingsflyt.sakogbehandling.sak.flate

import com.papsign.ktor.openapigen.annotations.parameters.PathParam

data class SaksnummerParameter(@param:PathParam("saksnummer") val saksnummer: String)