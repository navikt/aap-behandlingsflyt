package no.nav.aap.behandlingsflyt.behandling.dokumentinnhenting

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import com.zaxxer.hikari.HikariDataSource
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.dokumentinnhenting.BrevRequest
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.dokumentinnhenting.BrevResponse
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.dokumentinnhenting.DokumeninnhentingGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.dokumentinnhenting.LegeerklæringBestillingRequest
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.dokumentinnhenting.LegeerklæringStatusResponse
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.PdlPersoninfoGateway
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.httpklient.auth.token

fun NormalOpenAPIRoute.dokumentinnhentingAPI(dataSource: HikariDataSource) {
    route("/api/dokumentinnhenting/syfo") {
        route("/bestill") {
            post<Unit, String, BestillLegeerklæringDto> { _, req ->
                val bestilling = dataSource.transaction(readOnly = true) { connection ->

                    val sakService = SakService(connection)
                    val sak = sakService.hent(Saksnummer(req.saksnummer))
                    val personIdent = sak.person.aktivIdent()
                    //val personinfo = PdlPersoninfoGateway.hentPersoninfoForIdent(personIdent, token())

                    DokumeninnhentingGateway().bestillLegeerklæring(
                        LegeerklæringBestillingRequest(
                            req.behandlerRef,
                            req.behandlerNavn,
                            req.veilederNavn,
                            personIdent.identifikator,
                            "personinfo.fulltNavn()",
                            req.fritekst,
                            req.saksnummer,
                            req.dokumentasjonType
                        )
                    )
                }
                respond(bestilling)
            }
        }
        route("/status/{saksnummer}") {
            get<HentStatusLegeerklæring, List<LegeerklæringStatusResponse>> { par ->
                val status = DokumeninnhentingGateway().legeerklæringStatus(par.saksnummer)
                respond(status)
            }
        }
        route("/brevpreview") {
            post<Unit, BrevResponse, ForhåndsvisBrevRequest> { _, req ->
                val brevPreview = dataSource.transaction(readOnly = true) { connection ->
                    val sakService = SakService(connection)
                    val sak = sakService.hent(Saksnummer(req.saksnummer))

                    val personIdent = sak.person.aktivIdent()
                    //val personinfo = PdlPersoninfoGateway.hentPersoninfoForIdent(personIdent, token())

                    val brevRequest = BrevRequest("personinfo.fulltNavn()", personIdent.identifikator, req.fritekst, req.veilederNavn, req.dokumentasjonType)
                    DokumeninnhentingGateway().forhåndsvisBrev(brevRequest)
                }
                respond(BrevResponse(brevPreview))
            }
        }
    }
}