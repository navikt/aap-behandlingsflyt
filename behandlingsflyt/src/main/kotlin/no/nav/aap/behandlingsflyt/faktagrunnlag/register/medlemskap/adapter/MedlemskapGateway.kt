package no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.adapter

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.MedlemskapGateway
import no.nav.aap.behandlingsflyt.prometheus
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.Header
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.request.GetRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.lookup.gateway.Factory
import java.net.URI

class MedlemskapGateway : MedlemskapGateway {
    private val url = requiredConfigForKey("integrasjon.medl.url")
    private val config = ClientConfig(scope = requiredConfigForKey("integrasjon.medl.scope"))

    private val client = RestClient.withDefaultResponseHandler(
        config = config,
        tokenProvider = ClientCredentialsTokenProvider,
        prometheus = prometheus
    )

    companion object : Factory<MedlemskapGateway>{
        override fun konstruer(): MedlemskapGateway {
            return MedlemskapGateway()
        }
    }

    private fun query(request: MedlemskapRequest): List<MedlemskapResponse> {
        val urlWithParam = URI.create(url+"?fraOgMed=${request.periode.fom}&tilOgMed=${request.periode.tom}")

        val httpRequest = GetRequest(
            additionalHeaders = listOf(
                Header("Nav-Consumer-Id", "aap-behandlingsflyt"),
                Header("Nav-Personident", request.ident),
                Header("Accept", "application/json"),
            )
        )

        return requireNotNull(
            client.get(
                uri = urlWithParam,
                request = httpRequest,
                mapper = { body, _ ->
                    DefaultJsonMapper.fromJson(body)
                },
            )
        )
    }

    override fun innhent(person: Person, periode: Periode): List<MedlemskapResponse> {
        val request = MedlemskapRequest(
            ident = person.aktivIdent().identifikator,
            periode = periode
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
                helsedel = it.helsedel,
                lovvalgsland = it.lovvalgsland?.uppercase(),
            )
        }
    }
}