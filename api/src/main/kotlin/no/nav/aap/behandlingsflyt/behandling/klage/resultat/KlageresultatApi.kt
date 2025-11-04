package no.nav.aap.behandlingsflyt.behandling.klage.resultat

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.behandling.svarfraandreinstans.svarfraandreinstans.tilDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.resultat.KabalKlageresultatUtleder
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.resultat.KlageResultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.resultat.KlageresultatUtleder
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

fun NormalOpenAPIRoute.klageresultatApi(
    dataSource: DataSource,
    repositoryRegistry: RepositoryRegistry
) {
    route("api/klage/{referanse}/resultat") {
        authorizedGet<BehandlingReferanse, KlageResultat>(
            AuthorizationParamPathConfig(
                relevanteIdenterResolver = relevanteIdenterForBehandlingResolver(repositoryRegistry, dataSource),
                behandlingPathParam = BehandlingPathParam(
                    "referanse"
                )
            )
        ) { req ->
            val respons = dataSource.transaction(readOnly = true) { connection ->
                val repositoryProvider = repositoryRegistry.provider(connection)
                val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                val behandling = BehandlingReferanseService(behandlingRepository).behandling(req)
                KlageresultatUtleder(repositoryProvider).utledKlagebehandlingResultat(behandling.id)
            }
            respond(respons)
        }
    }

    route("api/klage/{referanse}/kabal-resultat") {
        authorizedGet<BehandlingReferanse, KabalKlageResultat>(
            AuthorizationParamPathConfig(
                relevanteIdenterResolver = relevanteIdenterForBehandlingResolver(repositoryRegistry, dataSource),
                behandlingPathParam = BehandlingPathParam(
                    "referanse"
                )
            )
        ) { req ->
            val respons = dataSource.transaction(readOnly = true) { connection ->
                val repositoryProvider = repositoryRegistry.provider(connection)
                val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                val mottattDokumentRepository = repositoryProvider.provide<MottattDokumentRepository>()
                val behandling = BehandlingReferanseService(behandlingRepository).behandling(req)
                val kabalHendelser = KabalKlageresultatUtleder(mottattDokumentRepository).hentKabalHendelserForKlage(behandling)
                KabalKlageResultat(kabalHendelser.map { it.tilDto() })
            }
            respond(respons)
        }
    }
}