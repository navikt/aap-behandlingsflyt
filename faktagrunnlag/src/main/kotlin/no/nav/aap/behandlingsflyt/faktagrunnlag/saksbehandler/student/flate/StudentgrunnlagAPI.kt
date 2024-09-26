package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.flate

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import com.zaxxer.hikari.HikariDataSource
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentRepository
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.komponenter.dbconnect.transaction

fun NormalOpenAPIRoute.studentgrunnlagApi(dataSource: HikariDataSource) {
    route("/api/behandling") {
        route("/{referanse}/grunnlag/student") {
            get<BehandlingReferanse, StudentGrunnlagDto> { req ->
                val studentGrunnlag: StudentGrunnlag? = dataSource.transaction { connection ->
                    val behandling = BehandlingReferanseService(BehandlingRepositoryImpl(connection)).behandling(req)

                    StudentRepository(connection).hentHvisEksisterer(behandlingId = behandling.id)
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