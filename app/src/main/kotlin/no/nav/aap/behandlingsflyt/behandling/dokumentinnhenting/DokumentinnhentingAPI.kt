package no.nav.aap.behandlingsflyt.behandling.dokumentinnhenting

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import com.zaxxer.hikari.HikariDataSource
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.dokumentinnhenting.DokumeninnhentingGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.dokumentinnhenting.LegeerklæringBestillingRequest
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.dokumentinnhenting.LegeerklæringStatusResponse
import no.nav.aap.komponenter.dbconnect.transaction

fun NormalOpenAPIRoute.dokumentinnhentingAPI(dataSource: HikariDataSource) {
    route("/api/dokumentinnhenting/syfo") {
        route("/bestill") {
            post<Unit, String, BestillLegeerklæringDto> { _, req ->
                val soningsgrunnlag = dataSource.transaction(readOnly = true) { connection ->
                    DokumeninnhentingGateway().bestillLegeerklæring(
                        LegeerklæringBestillingRequest(
                            req.behandlerRef,
                            "PERSONIDENT", // TODO: Vi må hente inn personident på en fin måte
                            req.fritekst,
                            req.sakId,
                            req.dokumentasjonType,
                            req.dialogmeldingVedlegg
                        )
                    )
                }
                respond(soningsgrunnlag.dialogmeldingUUID)
            }
        }
        route("/status/{saksnummer}") {
            get<HentStatusLegeerklæring, LegeerklæringStatusResponse> { par ->
                val status = DokumeninnhentingGateway().legeerklæringStatus(par.saksnummer)
                respond(status)
            }
        }
    }
}