package no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters

import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersoninfoGateway
import no.nav.aap.httpclient.ClientConfig
import no.nav.aap.httpclient.Header
import no.nav.aap.httpclient.RestClient
import no.nav.aap.httpclient.request.PostRequest
import no.nav.aap.httpclient.tokenprovider.OidcToken
import no.nav.aap.httpclient.tokenprovider.azurecc.OnBehalfOfTokenProvider
import no.nav.aap.pdl.IdentVariables
import no.nav.aap.pdl.PdlPersonNavnDataResponse
import no.nav.aap.pdl.PdlRequest
import no.nav.aap.pdl.PdlResponseHandler
import no.nav.aap.requiredConfigForKey
import no.nav.aap.verdityper.sakogbehandling.Ident
import java.net.URI

object PdlPersoninfoGateway : PersoninfoGateway {

    private const val ident = "\$ident"
    private val QUERY = """
    query($ident: ID!) {
        hentPerson(ident: $ident) {
            navn(historikk: false) {
                fornavn, mellomnavn, etternavn,
            }
        }
    }
""".trimIndent()

    private val url = URI.create(requiredConfigForKey("integrasjon.pdl.url"))
    val config = ClientConfig(
        scope = requiredConfigForKey("integrasjon.pdl.scope"),
        additionalHeaders = listOf(Header("Behandlingsnummer", "B287"))
    )
    private val client = RestClient(
        config = config,
        tokenProvider = OnBehalfOfTokenProvider,
        errorHandler = PdlResponseHandler(config = config)
    )

    private fun query(request: PdlRequest, currentToken: OidcToken): PdlPersonNavnDataResponse {
        val httpRequest = PostRequest(body = request, currentToken = currentToken)
        return requireNotNull(client.post(uri = url, request = httpRequest))
    }

    override fun hentPersoninfoForIdent(ident: Ident, currentToken: OidcToken): Personinfo {
        val request = PdlRequest(QUERY, IdentVariables(ident.identifikator))
        val response: PdlPersonNavnDataResponse = query(request, currentToken)
        val navn = response.data?.navn?.first()
        return Personinfo(ident, navn?.fornavn, navn?.mellomnavn, navn?.etternavn)
    }
}




