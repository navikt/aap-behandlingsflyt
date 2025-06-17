package no.nav.aap.behandlingsflyt.integrasjon.organisasjon

import no.nav.aap.behandlingsflyt.behandling.ansattinfo.Enhet
import no.nav.aap.behandlingsflyt.behandling.ansattinfo.EnhetGateway
import no.nav.aap.behandlingsflyt.behandling.ansattinfo.EnhetsType
import no.nav.aap.behandlingsflyt.prometheus
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.gateway.Factory
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.Header
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.request.GetRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.NoTokenTokenProvider
import no.nav.aap.komponenter.json.DefaultJsonMapper
import java.net.URI

class NorgGateway : EnhetGateway {
    private val baseUri = URI.create(requiredConfigForKey("integrasjon.norg.url"))
    private val client =
        RestClient.withDefaultResponseHandler(
            config = ClientConfig(),
            tokenProvider = NoTokenTokenProvider(),
            prometheus = prometheus
        )

    companion object : Factory<EnhetGateway> {
        override fun konstruer(): EnhetGateway = NorgGateway()
    }

    override fun hentEnhet(enhetsnummer: String): Enhet {
        val uriWithParams = URI.create("$baseUri/norg2/api/v1/enhet/$enhetsnummer")

        val httpRequest =
            GetRequest(
                additionalHeaders =
                    listOf(
                        Header("Accept", "application/json")
                    )
            )

        val response: NorgEnhet =
            checkNotNull(
                client.get(uri = uriWithParams, request = httpRequest, mapper = { body, _ ->
                    DefaultJsonMapper.fromJson(body)
                })
            )

        return response.tilEnhet()
    }

    override fun hentAlleEnheter(): List<Enhet> {
        val uriWithParams = URI.create("$baseUri/norg2/api/v1/enhet")

        val httpRequest =
            GetRequest(
                additionalHeaders =
                    listOf(
                        Header("Accept", "application/json")
                    )
            )

        val response: List<NorgEnhet> =
            checkNotNull(
                client.get(uri = uriWithParams, request = httpRequest, mapper = { body, _ ->
                    DefaultJsonMapper.fromJson<List<NorgEnhet>>(body)
                })
            )
        return response.tilEnhetListe()
    }

    private fun NorgEnhet.tilEnhet(): Enhet =
        Enhet(
            enhetsNummer = enhetNr,
            navn = navn,
            type =
                when (type) {
                    "LOKAL" -> EnhetsType.LOKAL
                    "YTA" -> EnhetsType.ARBEID_OG_YTELSE
                    "FYLKE" -> EnhetsType.FYLKE
                    else -> EnhetsType.ANNET
                }
        )


    private fun List<NorgEnhet>.tilEnhetListe(): List<Enhet> =
        this.map { it.tilEnhet() }
}