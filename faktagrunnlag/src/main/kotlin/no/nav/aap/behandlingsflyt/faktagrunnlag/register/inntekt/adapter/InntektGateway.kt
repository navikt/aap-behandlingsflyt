package no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.adapter

import no.nav.aap.Inntekt.InntektRequest
import no.nav.aap.Inntekt.InntektResponse
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.InntektPerÅr
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.InntektRegisterGateway
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.httpclient.ClientConfig
import no.nav.aap.httpclient.Header
import no.nav.aap.httpclient.RestClient
import no.nav.aap.httpclient.request.PostRequest
import no.nav.aap.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import no.nav.aap.requiredConfigForKey
import no.nav.aap.verdityper.Beløp
import java.net.URI
import java.time.Year

object InntektGateway : InntektRegisterGateway {
    private val url = URI.create(requiredConfigForKey("integrasjon.inntekt.url"))
    val config = ClientConfig(scope = requiredConfigForKey("integrasjon.inntekt.scope"))
    private val client = RestClient(
        config = config,
        tokenProvider = ClientCredentialsTokenProvider,
    )

    private fun query(request: InntektRequest): InntektResponse {
        val httpRequest = PostRequest(
            body = request,
            additionalHeaders = listOf(
                Header("Nav-Consumer-Id", "aap-behandlingsflyt"),
                Header("Accept", "application/json")
            )
        )
        return requireNotNull(client.post(uri = url, request = httpRequest))
    }

    override fun innhent(person: Person, år: Set<Year>): Set<InntektPerÅr> {
        val request = InntektRequest(
            person.identer().map { it.identifikator }.first(),
            tomAr = år.min().value,
            fomAr = år.max().value
        )
        val inntektRes = query(request)

        return inntektRes.inntekt.map { inntekt ->
            InntektPerÅr(
                Year.of(inntekt.inntektAr),
                Beløp(inntekt.belop)
            )
        }.toSet()
    }

}