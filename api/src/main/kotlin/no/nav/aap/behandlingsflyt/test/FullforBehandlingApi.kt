package no.nav.aap.behandlingsflyt.test

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import com.papsign.ktor.openapigen.route.tag
import no.nav.aap.behandlingsflyt.Tags
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepository
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.StudentStatus
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.SøknadV0
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.db.PersonRepository
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
    route("/api/test/opprettOgFullfoerBehandling").tag(Tags.Dolly) {
        @Suppress("UnauthorizedPost")
        post<Unit, OpprettOgFullforBehandlingRespons, OpprettOgFullforBehandlingRequest> { _, req ->
            require(!Miljø.erProd()) { "Ikke tilgjengelig i produksjonsmiljøet" }
            try {
                val sak = dataSource.transaction { connection ->
                    TestSakService(repositoryRegistry.provider(connection), gatewayProvider)
                        .opprettTestSak(
                            ident = Ident(req.ident),
                            erStudent = req.erStudent,
                            harYrkesskade = req.harYrkesskade,
                            harMedlemskap = req.harMedlemskap,
                            andreUtbetalinger = req.andreUtbetalinger?.tilKontrakt(),
                        )
                }
                thread(isDaemon = true) { service.fullforBehandling(sak) }
                respond(OpprettOgFullforBehandlingRespons(sak.saksnummer.toString()))
            } catch (e: OpprettTestSakException) {
                throw UgyldigForespørselException(message = e.message ?: "Ukjent feil", cause = e)
            }
        }
    }

    route("/api/test/behandlingStatus").tag(Tags.Dolly) {
        @Suppress("UnauthorizedPost")
        post<Unit, BehandlingStatusRespons, BehandlingStatusRequest> { _, req ->
            require(!Miljø.erProd()) { "Ikke tilgjengelig i produksjonsmiljøet" }
            val respons = dataSource.transaction(readOnly = true) { connection ->
                val provider = repositoryRegistry.provider(connection)
                val person = provider.provide<PersonRepository>().finn(Ident(req.ident))
                    ?: return@transaction BehandlingStatusRespons(req.ident, null, false)
                val sak = provider.provide<SakRepository>().finnSakerFor(person).firstOrNull()
                    ?: return@transaction BehandlingStatusRespons(req.ident, null, false)
                val behandling = BehandlingService(provider, gatewayProvider)
                    .finnSisteYtelsesbehandlingFor(sak.id)
                    ?: return@transaction BehandlingStatusRespons(sak.saksnummer.toString(), null, false)
                val status = behandling.status()

                val søknad = provider.provide<MottattDokumentRepository>()
                    .hentDokumenterAvType(behandling.id, InnsendingType.SØKNAD)
                    .firstOrNull()
                    ?.strukturerteData<SøknadV0>()
                    ?.data

                val soeknadDetaljer = søknad?.let {
                    SoeknadDetaljer(
                        erStudent = it.student?.erStudent == StudentStatus.Ja,
                        harYrkesskade = it.yrkesskade.equals("Ja", ignoreCase = true),
                        harMedlemskap = it.medlemskap?.harBoddINorgeSiste5År.equals("JA", ignoreCase = true),
                        andreUtbetalinger = it.andreUtbetalinger?.let { a -> AndreUtbetalingerApiDto.fraKontrakt(a) },
                    )
                }

                BehandlingStatusRespons(
                    saksnummer = sak.saksnummer.toString(),
                    behandlingStatus = status.name,
                    ferdig = status == Status.AVSLUTTET,
                    soeknad = soeknadDetaljer,
                )
            }
            respond(respons)
        }
    }
}

data class OpprettOgFullforBehandlingRespons(val saksnummer: String)

data class OpprettOgFullforBehandlingRequest(
    val ident: String,
    val erStudent: Boolean,
    val harYrkesskade: Boolean,
    val harMedlemskap: Boolean,
    val andreUtbetalinger: AndreUtbetalingerApiDto?
)

data class BehandlingStatusRequest(val ident: String)

data class BehandlingStatusRespons(
    val saksnummer: String,
    val behandlingStatus: String?,
    val ferdig: Boolean,
    val soeknad: SoeknadDetaljer? = null,
)

data class SoeknadDetaljer(
    val erStudent: Boolean,
    val harYrkesskade: Boolean, // dårlig navn,
    val harMedlemskap: Boolean,
    val andreUtbetalinger: AndreUtbetalingerApiDto?,
)

/*
 *   - spørre på fnr i stedet
 *   - droppe æøå
 *   - fullfor -> follfoer
 */