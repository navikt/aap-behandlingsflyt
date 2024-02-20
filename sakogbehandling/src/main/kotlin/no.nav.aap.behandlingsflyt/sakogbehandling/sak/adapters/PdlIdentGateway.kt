package no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters

import no.nav.aap.behandlingsflyt.sakogbehandling.sak.IdentGateway
import no.nav.aap.httpclient.ClientConfig
import no.nav.aap.httpclient.RestClient
import no.nav.aap.httpclient.request.PostRequest
import no.nav.aap.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import no.nav.aap.pdl.IdentVariables
import no.nav.aap.pdl.PdlGruppe
import no.nav.aap.pdl.PdlIdenterDataResponse
import no.nav.aap.pdl.PdlRequest
import no.nav.aap.requiredConfigForKey
import no.nav.aap.verdityper.sakogbehandling.Ident
import java.net.URI

object PdlIdentGateway : IdentGateway {

    private val url = URI.create(requiredConfigForKey("integrasjon.pdl.url"))
    private val client = RestClient(
        config = ClientConfig(scope = requiredConfigForKey("integrasjon.pdl.scope")),
        tokenProvider = ClientCredentialsTokenProvider
    )

    private fun query(request: PdlRequest): PdlIdenterDataResponse {
        val httpRequest = PostRequest(body = request, responseClazz = PdlIdenterDataResponse::class.java)
        return requireNotNull(client.post(uri = url, request = httpRequest))
    }

    override fun hentAlleIdenterForPerson(ident: Ident): List<Ident> {
        val request = PdlRequest(IDENT_QUERY, IdentVariables(ident.identifikator))
        val response: PdlIdenterDataResponse = query(request)

        return response.data
                ?.hentIdenter
                ?.identer
                ?.filter { it.gruppe == PdlGruppe.FOLKEREGISTERIDENT }
                ?.map { Ident(identifikator = it.ident, aktivIdent = it.historisk.not()) }
                ?: emptyList()
    }
}

private const val ident = "\$ident"

val IDENT_QUERY = """
    query($ident: ID!) {

        hentIdenter(ident: $ident, historikk: true) {
            identer {
                ident,
                historisk,
                gruppe
            }
        }
    }
""".trimIndent()
