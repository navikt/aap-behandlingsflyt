package no.nav.aap.behandlingsflyt.behandling.klage.fullmektig

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.behandling.ansattinfo.AnsattInfoService
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.fullmektig.FullmektigRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.FASTSETT_FULLMEKTIG_KODE
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.tilgang.kanSaksbehandle
import no.nav.aap.behandlingsflyt.tilgang.relevanteIdenterForBehandlingResolver
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.tilgang.BehandlingPathParam
import no.nav.aap.tilgang.getGrunnlag
import javax.sql.DataSource

fun NormalOpenAPIRoute.fullmektigGrunnlagApi(
    dataSource: DataSource,
    repositoryRegistry: RepositoryRegistry,
    gatewayProvider: GatewayProvider
) {
    val ansattInfoService = AnsattInfoService(gatewayProvider)
    route("api/klage/{referanse}/grunnlag/fullmektig") {
        getGrunnlag<BehandlingReferanse, FullmektigGrunnlagDto>(
            relevanteIdenterResolver = relevanteIdenterForBehandlingResolver(repositoryRegistry, dataSource),
            behandlingPathParam = BehandlingPathParam("referanse"),
            avklaringsbehovKode = FASTSETT_FULLMEKTIG_KODE
        ) { req ->
            val respons = dataSource.transaction(readOnly = true) { connection ->
                val repositoryProvider = repositoryRegistry.provider(connection)
                val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                val fullmektigRepository = repositoryProvider.provide<FullmektigRepository>()

                val behandling = behandlingRepository.hent(req)
                val grunnlag = fullmektigRepository.hentHvisEksisterer(behandling.id)
                grunnlag?.tilDto(kanSaksbehandle(), ansattInfoService)
                    ?: FullmektigGrunnlagDto(harTilgangTilÅSaksbehandle = kanSaksbehandle())
            }
            respond(respons)
        }
    }
}