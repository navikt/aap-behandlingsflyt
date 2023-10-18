package no.nav.aap.behandlingsflyt.grunnlag.bistand.flate

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.domene.ElementNotFoundException
import no.nav.aap.behandlingsflyt.domene.behandling.Behandling
import no.nav.aap.behandlingsflyt.domene.behandling.BehandlingTjeneste
import no.nav.aap.behandlingsflyt.flate.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.grunnlag.bistand.BistandsTjeneste
import java.util.*

fun NormalOpenAPIRoute.bistandsgrunnlagApi() {
    route("/api/behandling") {
        route("/{referanse}/grunnlag/bistand") {
            get<BehandlingReferanse, BistandGrunnlagDto> { req ->
                val behandling = behandling(req)

                val bistandsGrunnlag = BistandsTjeneste.hentHvisEksisterer(behandling.id)
                respond(BistandGrunnlagDto(bistandsGrunnlag?.vurdering))
            }
        }
    }
}

private fun behandling(req: BehandlingReferanse): Behandling {
    val eksternReferanse: UUID
    try {
        eksternReferanse = req.ref()
    } catch (exception: IllegalArgumentException) {
        throw ElementNotFoundException()
    }

    val behandling = BehandlingTjeneste.hent(eksternReferanse)
    return behandling
}