package no.nav.aap.behandlingsflyt.behandling.underveis

import com.papsign.ktor.openapigen.route.info
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.behandlingsflyt.tilgang.relevanteIdenterForBehandlingResolver
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.tilgang.AuthorizationParamPathConfig
import no.nav.aap.tilgang.BehandlingPathParam
import no.nav.aap.tilgang.authorizedGet
import javax.sql.DataSource

fun NormalOpenAPIRoute.underveisVurderingerAPI(datasource: DataSource, repositoryRegistry: RepositoryRegistry) {
    route("/api/behandling/underveis/{referanse}").authorizedGet<BehandlingReferanse, List<UnderveisperiodeDto>>(
        AuthorizationParamPathConfig(
            behandlingPathParam = BehandlingPathParam("referanse"),
            relevanteIdenterResolver = relevanteIdenterForBehandlingResolver(repositoryRegistry, datasource),
        ),
        null,
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
            val repositoryProvider = repositoryRegistry.provider(conn)
            val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
            val behandling =
                BehandlingReferanseService(behandlingRepository).behandling(behandlingReferanse)
            val underveisRepository = repositoryProvider.provide<UnderveisRepository>()
            underveisRepository.hentHvisEksisterer(behandling.id)
        }

        if (underveisGrunnlag == null) {
            respond(emptyList())
        } else {
            respond(underveisGrunnlag.perioder.map(::UnderveisperiodeDto))
        }
    }
}