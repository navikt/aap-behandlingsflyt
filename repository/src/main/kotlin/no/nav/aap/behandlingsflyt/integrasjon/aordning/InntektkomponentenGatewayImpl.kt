package no.nav.aap.behandlingsflyt.integrasjon.aordning

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.aordning.ArbeidsInntektInformasjon
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.aordning.ArbeidsInntektMaaned
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.aordning.Inntekt
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.aordning.InntektkomponentenGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.aordning.InntektskomponentData
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.aordning.Virksomhet
import no.nav.aap.behandlingsflyt.prometheus
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.gateway.Factory
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.Header
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.post
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import java.net.URI
import java.time.LocalDate
import java.time.YearMonth

/**
 * Se [Swagger](https://ikomp-q2.intern.dev.nav.no/swagger-ui/index.html?urls.primaryName=V1#/Legacy) for responstyper.
 */
class InntektkomponentenGatewayImpl : InntektkomponentenGateway {
    private val url = URI.create(requiredConfigForKey("integrasjon.inntektskomponenten.url") + "/hentinntektliste")
    private val config =
        ClientConfig(scope = requiredConfigForKey("integrasjon.inntektskomponenten.scope"))

    private val client = RestClient.withDefaultResponseHandler(
        config = config,
        tokenProvider = ClientCredentialsTokenProvider,
        prometheus = prometheus
    )

    companion object : Factory<InntektkomponentenGatewayImpl> {
        override fun konstruer(): InntektkomponentenGatewayImpl {
            return InntektkomponentenGatewayImpl()
        }
    }

    private fun query(request: InntektskomponentRequest): InntektskomponentResponse {
        val httpRequest = PostRequest(
            body = request,
            additionalHeaders = listOf(
                Header("Accept", "application/json")
            )
        )
        return requireNotNull(client.post(uri = url, request = httpRequest))
    }

    override fun hentAInntekt(fnr: String, fom: YearMonth, tom: YearMonth): InntektskomponentData {
        val request = InntektskomponentRequest(
            maanedFom = fom,
            maanedTom = tom,
            ident = Ident(fnr)
        )

        try {
            val response = query(request)
            return mapFraRespons(response)
        } catch (e: Exception) {
            throw RuntimeException("Feil ved henting av data i Inntektskomponenten: ${e.message}, $e")
        }
    }
}

private fun mapFraRespons(response: InntektskomponentResponse): InntektskomponentData {
    val rensetInnektliste =
        response.arbeidsInntektMaaned.filter { it.arbeidsInntektInformasjon.inntektListe?.isNotEmpty() == true }

    return InntektskomponentData(
        rensetInnektliste.map {
            ArbeidsInntektMaaned(
                aarMaaned = it.aarMaaned,
                arbeidsInntektInformasjon = ArbeidsInntektInformasjon(
                    inntektListe = it.arbeidsInntektInformasjon.inntektListe?.map { inntekt ->
                        Inntekt(
                            beloep = inntekt.beloep,
                            opptjeningsland = inntekt.opptjeningsland,
                            skattemessigBosattLand = inntekt.skattemessigBosattLand,
                            opptjeningsperiodeFom = inntekt.opptjeningsperiodeFom,
                            opptjeningsperiodeTom = inntekt.opptjeningsperiodeTom,
                            virksomhet = Virksomhet(inntekt.virksomhet.identifikator),
                            beskrivelse = inntekt.beskrivelse
                        )
                    }.orEmpty()
                )
            )
        }
    )
}

internal data class InntektskomponentRequest(
    val maanedFom: YearMonth,
    val maanedTom: YearMonth,
    val ident: Ident,
    val formaal: String = "Arbeidsavklaringspenger",
    val ainntektsfilter: String = "ArbeidsavklaringspengerA-inntekt"
)

internal data class Ident(
    val identifikator: String,
    val aktoerType: String = "NATURLIG_IDENT"
)

internal data class InntektskomponentResponse(
    val arbeidsInntektMaaned: List<ArbeidsInntektMaanedResponse> = emptyList()
)

internal data class ArbeidsInntektMaanedResponse(
    val aarMaaned: YearMonth,
    val arbeidsInntektInformasjon: ArbeidsInntektInformasjonResponse
)

internal data class ArbeidsInntektInformasjonResponse(
    val inntektListe: List<InntektResponse>? = emptyList()
)

internal data class InntektResponse(
    val beloep: Double,
    val opptjeningsland: String?,
    val skattemessigBosattLand: String?,
    val opptjeningsperiodeFom: LocalDate?,
    val opptjeningsperiodeTom: LocalDate?,
    val virksomhet: VirksomhetResponse,
    val beskrivelse: String?
)

internal data class VirksomhetResponse(
    val identifikator: String,
)