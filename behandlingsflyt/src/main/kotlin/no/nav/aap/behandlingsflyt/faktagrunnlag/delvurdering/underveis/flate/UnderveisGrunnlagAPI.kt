package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.flate

import com.papsign.ktor.openapigen.route.info
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.response.respondWithStatus
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.*
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.repository.RepositoryFactory
import javax.sql.DataSource

fun NormalOpenAPIRoute.underveisVurderingerAPI(datasource: DataSource) {
    route("/api/behandling/underveis/{referanse}").get<BehandlingReferanse, List<UnderveisperiodeDto>>(
        info(
            summary = "Hente alle underveis-vurderinger på en behandling",
            description = """
                * periode: Perioden denne vurdering gjelder for 
                * meldePeriode: Meldeperioden denne vurderingen faller inn i
                * trekk: Total trekk for hele perioden
            """.trimIndent()
        )
    ) { behandlingReferanse ->
        val underveisGrunnlag = datasource.transaction(readOnly = true) { conn ->
            val repositoryFactory = RepositoryFactory(conn)
            val behandlingRepository = repositoryFactory.create(BehandlingRepository::class)
            val behandling = BehandlingReferanseService(behandlingRepository).behandling(behandlingReferanse)
            UnderveisRepository(conn).hentHvisEksisterer(behandling.id)
        }

        if (underveisGrunnlag == null) {
            respondWithStatus(HttpStatusCode.NotFound)
        } else {
            respond(underveisGrunnlag.perioder.map(::UnderveisperiodeDto))
        }
    }
}