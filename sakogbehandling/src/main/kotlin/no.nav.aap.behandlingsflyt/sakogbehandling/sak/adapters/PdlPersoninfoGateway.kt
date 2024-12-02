package no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters

import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersoninfoGateway
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.Header
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.OnBehalfOfTokenProvider
import no.nav.aap.komponenter.httpklient.json.DefaultJsonMapper
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import java.net.URI

object PdlPersoninfoGateway : PersoninfoGateway {

    private const val ident = "\$ident"
    val PERSONINFO_QUERY = """
    query($ident: ID!) {
        hentPerson(ident: $ident) {
            navn(historikk: false) {
                fornavn, mellomnavn, etternavn,
            }
        }
    }
""".trimIndent()

    private const val identer = "\$identer"
    val PERSONINFO_BOLK_QUERY = """
        query($identer: [ID!]!) {
            hentPersonBolk(identer: $identer) {
                ident,
                person {
                    navn(historikk: false) {
                        fornavn
                        mellomnavn
                        etternavn
                    }
                },
                code
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
        responseHandler = PdlResponseHandler()
    )

    private fun query(request: PdlRequest, currentToken: OidcToken): PdlPersonNavnDataResponse {
        val httpRequest = PostRequest(body = request, currentToken = currentToken)
        return requireNotNull(client.post(uri = url, request = httpRequest, mapper = { body, _ ->
            DefaultJsonMapper.fromJson(body)
        }))
    }

    override fun hentPersoninfoForIdent(ident: Ident, currentToken: OidcToken): Personinfo {
        val request = PdlRequest(PERSONINFO_QUERY, IdentVariables(ident.identifikator))
        val response: PdlPersonNavnDataResponse = query(request, currentToken)
        val navn = response.data?.hentPerson?.navn?.firstOrNull()
        return Personinfo(ident, navn?.fornavn, navn?.mellomnavn, navn?.etternavn)
    }

}




