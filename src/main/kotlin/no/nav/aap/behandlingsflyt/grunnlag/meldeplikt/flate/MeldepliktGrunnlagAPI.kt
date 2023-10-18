package no.nav.aap.behandlingsflyt.grunnlag.meldeplikt.flate

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.domene.ElementNotFoundException
import no.nav.aap.behandlingsflyt.domene.behandling.Behandling
import no.nav.aap.behandlingsflyt.domene.behandling.BehandlingTjeneste
import no.nav.aap.behandlingsflyt.flate.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.grunnlag.meldeplikt.MeldepliktTjeneste
import java.util.*

fun NormalOpenAPIRoute.meldepliktsgrunnlagApi() {
    route("/api/behandling") {
        route("/{referanse}/grunnlag/fritak-meldeplikt") {
            get<BehandlingReferanse, FritakMeldepliktGrunnlagDto> { req ->
                val behandling = behandling(req)

                val meldepliktGrunnlag = MeldepliktTjeneste.hentHvisEksisterer(behandling.id)
                respond(FritakMeldepliktGrunnlagDto(meldepliktGrunnlag?.vurderinger.orEmpty()))
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