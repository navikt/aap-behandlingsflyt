package no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters

import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.Header
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import no.nav.aap.komponenter.httpklient.json.DefaultJsonMapper
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import java.net.URI
import kotlin.collections.map

object PdlPersoninfoBulkGateway {

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
        tokenProvider = ClientCredentialsTokenProvider,
        responseHandler = PdlResponseHandler()
    )

    private fun query(request: PdlRequest): PdlPersonNavnDataResponse {
        val httpRequest = PostRequest(body = request)
        return requireNotNull(client.post(uri = url, request = httpRequest, mapper = { body, _ ->
            DefaultJsonMapper.fromJson(body)
        }))
    }

    fun hentPersoninfoForIdenter(identer: List<Ident>): List<Personinfo> {
        val request = PdlRequest(PERSONINFO_BOLK_QUERY, IdentVariables(identer = identer.map { it.identifikator }))
        val response: PdlPersonNavnDataResponse = query(request)

        return response.data?.hentPersonBolk?.map { person -> mapPersoninformasjon(person) } ?: emptyList()
    }

    private fun mapPersoninformasjon(data: PdlNavnData): Personinfo {
        val navn = data.navn?.firstOrNull()

        if (navn == null) {
            return Personinfo(Ident(data.ident!!), "Ukjent", null, null)
        }

        return Personinfo(Ident(data.ident!!), navn.fornavn, navn.mellomnavn, navn.etternavn)
    }
}




