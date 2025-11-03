package no.nav.aap.behandlingsflyt.behandling.klage.klagebehandling

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.behandling.ansattinfo.AnsattInfoService
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.klagebehandling.nay.KlagebehandlingNayRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.VURDER_KLAGE_NAY_KODE
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
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

fun NormalOpenAPIRoute.klagebehandlingNayGrunnlagApi(
    dataSource: DataSource,
    repositoryRegistry: RepositoryRegistry,
    gatewayProvider: GatewayProvider
) {
    val ansattInfoService = AnsattInfoService(gatewayProvider)
    route("api/klage/{referanse}/grunnlag/klagebehandling-nay") {
        getGrunnlag<BehandlingReferanse, KlagebehandlingNayGrunnlagDto>(
            relevanteIdenterResolver = relevanteIdenterForBehandlingResolver(repositoryRegistry, dataSource),
            behandlingPathParam = BehandlingPathParam("referanse"),
            avklaringsbehovKode = VURDER_KLAGE_NAY_KODE
        ) { req ->
            val respons = dataSource.transaction(readOnly = true) { connection ->
                val repositoryProvider = repositoryRegistry.provider(connection)
                val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                val klagebehandlingNayRepository = repositoryProvider.provide<KlagebehandlingNayRepository>()

                val behandling: Behandling =
                    BehandlingReferanseService(behandlingRepository).behandling(req)

                klagebehandlingNayRepository.hentHvisEksisterer(behandling.id)
                    ?.tilDto(kanSaksbehandle(), ansattInfoService)
                    ?: KlagebehandlingNayGrunnlagDto(harTilgangTilÅSaksbehandle = kanSaksbehandle())
            }
            respond(respons)
        }
    }
}