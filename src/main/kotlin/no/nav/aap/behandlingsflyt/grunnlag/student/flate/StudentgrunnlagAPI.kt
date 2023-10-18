package no.nav.aap.behandlingsflyt.grunnlag.student.flate

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.domene.ElementNotFoundException
import no.nav.aap.behandlingsflyt.domene.behandling.Behandling
import no.nav.aap.behandlingsflyt.domene.behandling.BehandlingTjeneste
import no.nav.aap.behandlingsflyt.flate.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.grunnlag.student.StudentTjeneste
import java.util.*

fun NormalOpenAPIRoute.studentgrunnlagApi() {
    route("/api/behandling") {
        route("/{referanse}/grunnlag/student") {
            get<BehandlingReferanse, StudentGrunnlagDto> { req ->
                val behandling = behandling(req)

                val studentGrunnlag = StudentTjeneste.hentHvisEksisterer(behandlingId = behandling.id)

                respond(StudentGrunnlagDto(
                    studentvurdering = studentGrunnlag?.studentvurdering
                ))
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