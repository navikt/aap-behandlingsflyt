package no.nav.aap.behandlingsflyt.integrasjon.ident

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.BarnGateway
import no.nav.aap.behandlingsflyt.integrasjon.barn.PdlBarnGateway
import no.nav.aap.behandlingsflyt.prometheus
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.IdentGateway
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.IdentVariables
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.PdlGruppe
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.PdlIdenterDataResponse
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.PdlRequest
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.PdlResponseHandler
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.gateway.Factory
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.Header
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import no.nav.aap.komponenter.json.DefaultJsonMapper
import java.net.URI

class PdlIdentGateway : IdentGateway {

    private val url = URI.create(requiredConfigForKey("integrasjon.pdl.url"))
    val config = ClientConfig(
        scope = requiredConfigForKey("integrasjon.pdl.scope"),
        additionalHeaders = listOf(Header("Behandlingsnummer", "B287"))
    )
    private val client = RestClient(
        config = config,
        tokenProvider = ClientCredentialsTokenProvider,
        responseHandler = PdlResponseHandler(),
        prometheus = prometheus
    )

    companion object : Factory<IdentGateway> {
        override fun konstruer(): PdlIdentGateway {
            return PdlIdentGateway()
        }
    }

    private fun query(request: PdlRequest): PdlIdenterDataResponse {
        val httpRequest = PostRequest(body = request)
        return requireNotNull(client.post(uri = url, request = httpRequest, mapper = { body, _ ->
            DefaultJsonMapper.fromJson(body)
        }))
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

