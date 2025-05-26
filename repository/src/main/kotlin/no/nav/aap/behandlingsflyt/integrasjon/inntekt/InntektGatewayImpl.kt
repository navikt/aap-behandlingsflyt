package no.nav.aap.behandlingsflyt.integrasjon.inntekt

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.InntektPerÅr
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.InntektRegisterGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.adapter.InntektRequest
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.adapter.InntektResponse
import no.nav.aap.behandlingsflyt.prometheus
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.Header
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.komponenter.verdityper.Beløp
import java.net.URI
import java.time.Year


object InntektGatewayImpl : InntektRegisterGateway {
    private val url = URI.create(requiredConfigForKey("integrasjon.inntekt.url"))
    val config = ClientConfig(scope = requiredConfigForKey("integrasjon.inntekt.scope"))

    private val client = RestClient.withDefaultResponseHandler(
        config = config,
        tokenProvider = ClientCredentialsTokenProvider,
        prometheus = prometheus
    )

    private fun query(request: InntektRequest): InntektResponse {
        val httpRequest = PostRequest(
            body = request,
            additionalHeaders = listOf(
                Header("Nav-Consumer-Id", "aap-behandlingsflyt"),
                Header("Accept", "application/json")
            )
        )
        val inntektResponse: InntektResponse? = client.post(uri = url, request = httpRequest, mapper = { body, _ ->
            DefaultJsonMapper.fromJson(body)
        })

        if (inntektResponse == null) {
            return InntektResponse(emptyList())
        }
        return inntektResponse
    }

    override fun innhent(person: Person, år: Set<Year>): Set<InntektPerÅr> {
        return person.identer().map { ident ->
            val request = InntektRequest(
                ident.identifikator,
                fomAr = år.min().value,
                tomAr = år.max().value
            )
            query(request)
        }
            .flatMap { it.inntekter }
            .map { inntekt ->
                InntektPerÅr(
                    Year.of(inntekt.inntektAr),
                    Beløp(inntekt.belop)
                )
            }.toSet()
    }

}