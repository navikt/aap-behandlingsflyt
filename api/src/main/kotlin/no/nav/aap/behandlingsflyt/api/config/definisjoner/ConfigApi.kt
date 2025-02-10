package no.nav.aap.behandlingsflyt.api.config.definisjoner

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon

fun NormalOpenAPIRoute.configApi() {
    route("/config/definisjoner") {
        get<Unit, Map<AvklaringsbehovKode, Definisjon>> {
            val response = HashMap<AvklaringsbehovKode, Definisjon>()
            Definisjon.entries.forEach {
                response[it.kode] = it
            }
            respond(response)
        }
    }
}