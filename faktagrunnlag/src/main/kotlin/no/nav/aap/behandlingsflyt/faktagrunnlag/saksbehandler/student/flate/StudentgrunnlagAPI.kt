package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.flate

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import com.zaxxer.hikari.HikariDataSource
import no.nav.aap.behandlingsflyt.dbconnect.transaction
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService

fun NormalOpenAPIRoute.studentgrunnlagApi(dataSource: HikariDataSource) {
    route("/api/behandling") {
        route("/{referanse}/grunnlag/student") {
            get<BehandlingReferanse, StudentGrunnlagDto> { req ->
                val behandling: Behandling = dataSource.transaction {
                    BehandlingReferanseService(it).behandling(req)
                }

                val studentGrunnlag = dataSource.transaction {
                    StudentRepository(it).hentHvisEksisterer(behandlingId = behandling.id)
                }

                if (studentGrunnlag != null) {
                    respond(
                        StudentGrunnlagDto(
                            studentvurdering = studentGrunnlag.studentvurdering,
                            oppgittStudent = studentGrunnlag.oppgittStudent
                        )
                    )
                } else {
                    respond(NoneStudentGrunnlagDto())
                }

            }
        }
    }
}