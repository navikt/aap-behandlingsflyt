package no.nav.aap.behandlingsflyt.grunnlag.student.flate

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.flate.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.grunnlag.BehandlingReferanseService
import no.nav.aap.behandlingsflyt.grunnlag.student.db.InMemoryStudentRepository

fun NormalOpenAPIRoute.studentgrunnlagApi() {
    route("/api/behandling") {
        route("/{referanse}/grunnlag/student") {
            get<BehandlingReferanse, StudentGrunnlagDto> { req ->
                val behandling = BehandlingReferanseService.behandling(req)

                val studentGrunnlag = InMemoryStudentRepository.hentHvisEksisterer(behandlingId = behandling.id)

                respond(StudentGrunnlagDto(
                    studentvurdering = studentGrunnlag?.studentvurdering
                ))
            }
        }
    }
}