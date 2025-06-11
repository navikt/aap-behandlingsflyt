package no.nav.aap.behandlingsflyt

import com.papsign.ktor.openapigen.route.TagModule
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import com.papsign.ktor.openapigen.route.tag
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.DokumentInfoId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.Journalpost
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.SafHentDokumentGateway
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.SafListDokument
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.SafListDokumentGateway
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.flate.DokumentResponsDTO
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.flate.HentDokumentDTO
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.flate.HentSakDTO
import no.nav.aap.komponenter.httpklient.auth.token
import no.nav.aap.tilgang.AuthorizationBodyPathConfig
import no.nav.aap.tilgang.AuthorizationParamPathConfig
import no.nav.aap.tilgang.JournalpostPathParam
import no.nav.aap.tilgang.Operasjon
import no.nav.aap.tilgang.SakPathParam
import no.nav.aap.tilgang.authorizedGet
import no.nav.aap.tilgang.authorizedPost
import no.nav.aap.tilgang.plugin.kontrakt.Personreferanse
import no.nav.aap.verdityper.dokument.JournalpostId

fun NormalOpenAPIRoute.dokumentAPI() {
    route("/api/dokumenter").tag(Tags.Dokumenter) {
        route("/bruker") {
            authorizedPost<Unit, List<Journalpost>, HentDokumentoversiktBrukerDTO>(
                AuthorizationBodyPathConfig(operasjon = Operasjon.SE, applicationsOnly = false)
            ) { _, req ->
                val dokumenter = SafListDokumentGateway.hentDokumenterForBruker(req.personIdent, token())

                respond(dokumenter)
            }
        }

        route("/sak/{saksnummer}") {
            authorizedGet<HentSakDTO, List<SafListDokument>>(
                AuthorizationParamPathConfig(
                    sakPathParam = SakPathParam("saksnummer")
                ), null, TagModule(listOf(Tags.Sak))
            ) { req ->
                val token = token()
                val safRespons = SafListDokumentGateway.hentDokumenterForSak(Saksnummer(req.saksnummer), token)
                respond(
                    safRespons
                )
            }
        }

        route("/{journalpostId}/{dokumentinfoId}") {
            authorizedGet<HentDokumentDTO, DokumentResponsDTO>(
                AuthorizationParamPathConfig(
                    journalpostPathParam = JournalpostPathParam("journalpostId")
                )
            ) { req ->
                val journalpostId = req.journalpostId
                val dokumentInfoId = req.dokumentinfoId

                val token = token()
                val gateway = SafHentDokumentGateway.withDefaultRestClient()

                val dokumentRespons =
                    gateway.hentDokument(JournalpostId(journalpostId), DokumentInfoId(dokumentInfoId), token)

                pipeline.call.response.headers.append(
                    name = "Content-Disposition", value = "inline; filename=${dokumentRespons.filnavn}"
                )
                respond(DokumentResponsDTO(stream = dokumentRespons.dokument))
            }
        }
    }

}

private class HentDokumentoversiktBrukerDTO(
    val personIdent: String
) : Personreferanse {
    override fun hentPersonreferanse(): String = personIdent
}