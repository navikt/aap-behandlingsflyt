package no.nav.aap.behandlingsflyt.integrasjon.samordning

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.andrestatligeytelservurdering.gateway.TiltakspengerGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.andrestatligeytelservurdering.gateway.TiltakspengerKilde
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.andrestatligeytelservurdering.gateway.TiltakspengerPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.andrestatligeytelservurdering.gateway.TiltakspengerYtelseType
import no.nav.aap.behandlingsflyt.prometheus
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.gateway.Factory
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.Header
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.post
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.AzureM2MTokenProvider
import java.net.URI
import java.time.LocalDate

// https://tiltakspenger-datadeling.intern.dev.nav.no/swagger
class TiltakspengerGatewayImpl : TiltakspengerGateway {
    private val url = URI.create(requiredConfigForKey("integrasjon.tiltakspenger.url") + "/vedtak/perioder")
    private val config = ClientConfig(scope = requiredConfigForKey("integrasjon.tiltakspenger.scope"))

    companion object : Factory<TiltakspengerGateway> {
        override fun konstruer(): TiltakspengerGateway {
            return TiltakspengerGatewayImpl()
        }
    }

    private val client = RestClient.withDefaultResponseHandler(
        config = config, tokenProvider = AzureM2MTokenProvider, prometheus = prometheus
    )

    private fun query(request: TiltakspengerRequest): List<TiltakspengerVedtakResponse> {
        val httpRequest = PostRequest(
            body = request, additionalHeaders = listOf(
                Header("Accept", "application/json")
            )
        )

        val response: List<TiltakspengerVedtakResponse>? = client.post(uri = url, request = httpRequest)
        return requireNotNull(response)
    }

    override fun hentYtelseTiltakspenger(
        personidentifikatorer: String,
        fom: LocalDate,
        tom: LocalDate
    ): List<TiltakspengerPeriode> {
        return query(
            TiltakspengerRequest(
                ident = personidentifikatorer,
                fom = fom.toString(),
                tom = tom.toString()
            )
        ).map { vedtak ->
            TiltakspengerPeriode(
                fraOgMed = vedtak.periode.fraOgMed,
                tilOgMed = vedtak.periode.tilOgMed,
                kilde = vedtak.kilde,
                tiltakspengerYtelseType = vedtak.rettighet
            )
        }
    }
}

data class TiltakspengerRequest(
    val ident: String,
    val fom: String,
    val tom: String
)

data class TiltakspengerPeriodeResponse(
    val fraOgMed: LocalDate,
    val tilOgMed: LocalDate
)

data class TiltakspengerVedtakResponse(
    val rettighet: TiltakspengerYtelseType,
    val periode: TiltakspengerPeriodeResponse,
    val kilde: TiltakspengerKilde,
)
