package no.nav.aap.behandlingsflyt.test

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respondWithStatus
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.HttpStatusCode
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.AndreUtbetalingerDto
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.komponenter.miljo.Miljø
import no.nav.aap.komponenter.repository.RepositoryRegistry
import javax.sql.DataSource

fun NormalOpenAPIRoute.opprettDummySakApi(
    dataSource: DataSource,
    repositoryRegistry: RepositoryRegistry,
    gatewayProvider: GatewayProvider,
) {
    route("/api/test/opprettDummySak") {
        @Suppress("UnauthorizedPost") // bare tilgjengelig i DEV og lokalt
        post<Unit, Map<String, String>, OpprettDummySakDto> { _, req ->
            require(!Miljø.erProd()) {
                "Kan ikke opprette dummy-sak i produksjonsmiljøet"
            }
            try {
                dataSource.transaction { connection ->
                    val provider = repositoryRegistry.provider(connection)
                    validerIngenEksisterendeSaker(PersonOgSakService(gatewayProvider, provider), Ident(req.ident))
                    TestSakService(provider, gatewayProvider).opprettTestSak(
                        ident = Ident(req.ident),
                        erStudent = req.erStudent,
                        harYrkesskade = req.harYrkesskade,
                        harMedlemskap = req.harMedlemskap,
                        andreUtbetalinger = req.andreUtbetalinger
                    )
                }
                respondWithStatus(HttpStatusCode.Accepted)
            } catch (e: OpprettTestSakException) {
                throw UgyldigForespørselException(message = e.message ?: "Ukjent feil")
            }
        }
    }
}

private fun validerIngenEksisterendeSaker(personOgSakService: PersonOgSakService, ident: Ident) {
    val eksisterendeSak = personOgSakService.finnSakerFor(ident).firstOrNull() ?: return
    throw OpprettTestSakException(
        "Det finnes allerede en eller flere saker for bruker. " +
                "Fant sak med saksnummer: ${eksisterendeSak.saksnummer}. " +
                "Vennligst bruk en annen testbruker eller gjenbruk den åpne saken."
    )
}

data class OpprettDummySakDto(
    val ident: String,
    val erStudent: Boolean,
    val harYrkesskade: Boolean,
    val harMedlemskap: Boolean,
    val andreUtbetalinger: AndreUtbetalingerDto?
)