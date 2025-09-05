package no.nav.aap.behandlingsflyt.sakogbehandling.sak.flate

import com.papsign.ktor.openapigen.annotations.parameters.PathParam

data class SøkDto(@param:PathParam("soketekst") val søketekst: String)
