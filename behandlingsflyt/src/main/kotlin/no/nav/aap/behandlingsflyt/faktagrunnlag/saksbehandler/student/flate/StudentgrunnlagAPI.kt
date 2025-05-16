package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.flate

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.behandlingsflyt.tilgang.TilgangGateway
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.httpklient.auth.token
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.tilgang.AuthorizationParamPathConfig
import no.nav.aap.tilgang.BehandlingPathParam
import no.nav.aap.tilgang.authorizedGet
import javax.sql.DataSource

fun NormalOpenAPIRoute.studentgrunnlagApi(
    dataSource: DataSource,
    repositoryRegistry: RepositoryRegistry
) {
    route("/api/behandling") {
        route("/{referanse}/grunnlag/student") {
            authorizedGet<BehandlingReferanse, StudentGrunnlagResponse>(
                AuthorizationParamPathConfig(
                    behandlingPathParam =
                        BehandlingPathParam(
                            "referanse"
                        )
                )
            ) { req ->
                val studentGrunnlag: StudentGrunnlag? =
                    dataSource.transaction(readOnly = true) { connection ->
                        val repositoryProvider = repositoryRegistry.provider(connection)
                        val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                        val studentRepository = repositoryProvider.provide<StudentRepository>()
                        val behandling =
                            BehandlingReferanseService(behandlingRepository).behandling(req)

                        studentRepository.hentHvisEksisterer(behandlingId = behandling.id)
                    }

                val harTilgangTilÅSaksbehandle =
                    GatewayProvider.provide<TilgangGateway>().sjekkTilgangTilBehandling(
                        req.referanse,
                        Definisjon.AVKLAR_STUDENT,
                        token()
                    )

                if (studentGrunnlag != null) {
                    respond(
                        StudentGrunnlagResponse(
                            harTilgangTilÅSaksbehandle = harTilgangTilÅSaksbehandle,
                            studentvurdering = studentGrunnlag.studentvurdering?.tilResponse(),
                            oppgittStudent = studentGrunnlag.oppgittStudent
                        )
                    )
                } else {
                    respond(NoneStudentGrunnlagResponse())
                }
            }
        }
    }
}