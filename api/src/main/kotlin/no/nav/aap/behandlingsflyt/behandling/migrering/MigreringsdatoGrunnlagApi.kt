package no.nav.aap.behandlingsflyt.behandling.migrering

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvService
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.migrering.MigreringsdatoRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.behandlingsflyt.tilgang.kanSaksbehandle
import no.nav.aap.behandlingsflyt.tilgang.relevanteIdenterForBehandlingResolver
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.tilgang.BehandlingPathParam
import no.nav.aap.tilgang.getGrunnlag
import javax.sql.DataSource

fun NormalOpenAPIRoute.migreringsdatoGrunnlagApi(
    dataSource: DataSource,
    repositoryRegistry: RepositoryRegistry,
    gatewayProvider: GatewayProvider,
) {
    route("/api/behandling/{referanse}/grunnlag/migreringsdato") {
        getGrunnlag<BehandlingReferanse, MigreringsdatoGrunnlagResponse>(
            relevanteIdenterResolver = relevanteIdenterForBehandlingResolver(repositoryRegistry, dataSource),
            behandlingPathParam = BehandlingPathParam("referanse"),
            påkrevdRolle = Definisjon.AVKLAR_MIGRERINGSDATO.løsesAv,
        ) { req ->
            val response = dataSource.transaction(readOnly = true) { connection ->
                val repositoryProvider = repositoryRegistry.provider(connection)
                val behandling = BehandlingReferanseService(
                    repositoryProvider.provide<BehandlingRepository>()
                ).behandling(req)

                val grunnlag = repositoryProvider.provide<MigreringsdatoRepository>()
                    .hentHvisEksisterer(behandling.id)

                val vurdertAvService = VurdertAvService(repositoryProvider, gatewayProvider)

                MigreringsdatoGrunnlagResponse(
                    harTilgangTilÅSaksbehandle = kanSaksbehandle(),
                    vurdering = grunnlag?.gjeldendeVurdering()?.toResponse(vurdertAvService),
                )
            }
            respond(response)
        }
    }
}
