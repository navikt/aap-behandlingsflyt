package no.nav.aap.behandlingsflyt.test

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.komponenter.miljo.Miljø
import no.nav.aap.komponenter.repository.RepositoryRegistry
import javax.sql.DataSource
import kotlin.concurrent.thread

fun NormalOpenAPIRoute.fullførBehandlingApi(
    dataSource: DataSource,
    repositoryRegistry: RepositoryRegistry,
    gatewayProvider: GatewayProvider,
) {
    val service = TestBehandlingFullføringService(dataSource, repositoryRegistry, gatewayProvider)
    if (Miljø.erProd()) return
    route("/api/test/opprettOgFullforBehandling") {
        @Suppress("UnauthorizedPost")
        post<Unit, OpprettOgFullforBehandlingRespons, OpprettDummySakDto> { _, req ->
            require(!Miljø.erProd()) { "Ikke tilgjengelig i produksjonsmiljøet" }
            try {
                val sak = dataSource.transaction { connection ->
                    TestSakService(repositoryRegistry.provider(connection), gatewayProvider)
                        .opprettTestSak(
                            ident = Ident(req.ident),
                            erStudent = req.erStudent,
                            harYrkesskade = req.harYrkesskade,
                            harMedlemskap = req.harMedlemskap,
                            andreUtbetalinger = req.andreUtbetalinger,
                        )
                }
                thread(isDaemon = true) { service.fullforBehandling(sak) }
                respond(OpprettOgFullforBehandlingRespons(sak.saksnummer.toString()))
            } catch (e: OpprettTestSakException) {
                throw UgyldigForespørselException(message = e.message ?: "Ukjent feil", cause = e)
            }
        }
    }

    route("/api/test/behandlingStatus") {
        @Suppress("UnauthorizedPost")
        post<Unit, BehandlingStatusRespons, BehandlingStatusRequest> { _, req ->
            require(!Miljø.erProd()) { "Ikke tilgjengelig i produksjonsmiljøet" }
            val (behandlingStatus, erFerdig) = dataSource.transaction(readOnly = true) { connection ->
                val provider = repositoryRegistry.provider(connection)
                val sak = provider.provide<SakRepository>().hentHvisFinnes(Saksnummer(req.saksnummer))
                    ?: return@transaction Pair(null, false)
                val behandling = BehandlingService(provider, gatewayProvider)
                    .finnSisteYtelsesbehandlingFor(sak.id)
                    ?: return@transaction Pair(null, false)
                val status = behandling.status()
                Pair(status.name, status == Status.AVSLUTTET)
            }
            respond(BehandlingStatusRespons(req.saksnummer, behandlingStatus, erFerdig))
        }
    }
}

data class OpprettOgFullforBehandlingRespons(val saksnummer: String)

data class BehandlingStatusRequest(val saksnummer: String)

data class BehandlingStatusRespons(
    val saksnummer: String,
    val behandlingStatus: String?,
    val ferdig: Boolean,
)
