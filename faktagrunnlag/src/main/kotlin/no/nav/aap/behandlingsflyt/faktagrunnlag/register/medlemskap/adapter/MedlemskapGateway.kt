package no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.adapter

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.MedlemskapGateway
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.Header
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.request.GetRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import no.nav.aap.komponenter.httpklient.json.DefaultJsonMapper
import java.net.URI

class MedlemskapGateway : MedlemskapGateway {
    private val url = URI.create(requiredConfigForKey("integrasjon.medl.url"))
    private val config = ClientConfig(scope = requiredConfigForKey("integrasjon.medl.scope"))

    private val client = RestClient.withDefaultResponseHandler(
        config = config,
        tokenProvider = ClientCredentialsTokenProvider
    )

    private fun query(request: MedlemskapRequest): List<MedlemskapResponse> {
        val httpRequest = GetRequest(
            additionalHeaders = listOf(
                Header("Nav-Consumer-Id", "aap-behandlingsflyt"),
                Header("Nav-Personident", request.ident),
                Header("Accept", "application/json"),
            )
        )

        return requireNotNull(
            client.get(
                uri = url,
                request = httpRequest,
                mapper = { body, _ ->
                    DefaultJsonMapper.fromJson(body)
                }
            )
        )
    }

    override fun innhent(person: Person): List<MedlemskapResponse> {
        val request = MedlemskapRequest(
            ident = person.aktivIdent().identifikator
        )
        val medlemskapResultat = query(request)

        return medlemskapResultat.map {
            MedlemskapResponse(
                unntakId = it.unntakId,
                ident = it.ident,
                fraOgMed = it.fraOgMed,
                tilOgMed = it.tilOgMed,
                status = it.status,
                statusaarsak = it.statusaarsak,
                medlem = it.medlem,
                grunnlag = it.grunnlag,
                lovvalg = it.lovvalg,
                helsedel = it.helsedel
            )
        }
    }
}