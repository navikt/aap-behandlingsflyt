package no.nav.aap.behandlingsflyt.behandling.kvalitetssikring

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import com.papsign.ktor.openapigen.route.tag
import no.nav.aap.behandlingsflyt.Tags
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.behandlingsflyt.tilgang.kanSaksbehandle
import no.nav.aap.behandlingsflyt.tilgang.relevanteIdenterForBehandlingResolver
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.komponenter.server.auth.bruker
import no.nav.aap.tilgang.AuthorizationParamPathConfig
import no.nav.aap.tilgang.BehandlingPathParam
import no.nav.aap.tilgang.Operasjon
import no.nav.aap.tilgang.authorizedGet
import javax.sql.DataSource

fun NormalOpenAPIRoute.kvalitetssikringTilgangApi(dataSource: DataSource, repositoryRegistry: RepositoryRegistry) {
    route("/api/behandling/{referanse}/kvalitetssikring-tilgang").tag(Tags.Behandling) {
        authorizedGet<BehandlingReferanse, KvalitetssikringTilgangDto>(
            AuthorizationParamPathConfig(
                relevanteIdenterResolver = relevanteIdenterForBehandlingResolver(repositoryRegistry, dataSource),
                operasjonerIKontekst = listOf(Operasjon.SAKSBEHANDLE),
                avklaringsbehovKode = Definisjon.KVALITETSSIKRING.kode.toString(),
                behandlingPathParam = BehandlingPathParam(
                    "referanse"
                )
            )
        ) { req ->
            val dto = dataSource.transaction(readOnly = true) { connection ->
                val repositoryProvider = repositoryRegistry.provider(connection)
                val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                val avklaringsbehovRepository = repositoryProvider.provide<AvklaringsbehovRepository>()
                val behandling = behandling(behandlingRepository, req)
                val innloggetBruker = bruker()

                val avklaringsbehov = avklaringsbehovRepository.hentAvklaringsbehovene(behandlingId = behandling.id)
                val avklaringsbehovSomKreverKvalitetssikring = avklaringsbehov.alle().filter { it.kreverKvalitetssikring() }

                KvalitetssikringTilgangDto(
                    harTilgangTilÅKvalitetssikre = kanSaksbehandle() && !avklaringsbehovSomKreverKvalitetssikring.any {
                        it.brukere().contains(innloggetBruker.ident)
                    }
                )
            }
            respond(dto)
        }
    }
}

private fun behandling(behandlingRepository: BehandlingRepository, req: BehandlingReferanse): Behandling {
    return BehandlingReferanseService(behandlingRepository).behandling(req)
}
