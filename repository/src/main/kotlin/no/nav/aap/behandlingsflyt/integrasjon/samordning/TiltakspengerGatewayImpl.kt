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
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import java.net.URI
import java.time.LocalDate

// https://tiltakspenger-datadeling.intern.dev.nav.no/swagger
class TiltakspengerGatewayImpl: TiltakspengerGateway {
    private val url = URI.create(requiredConfigForKey("integrasjon.tiltakspenger.url") + "/vedtak/perioder")
    private val config = ClientConfig(scope = requiredConfigForKey("integrasjon.tiltakspenger.scope"))

    companion object : Factory<TiltakspengerGateway> {
        override fun konstruer(): TiltakspengerGateway {
            return TiltakspengerGatewayImpl()
        }
    }

    private val client = RestClient.withDefaultResponseHandler(
        config = config, tokenProvider = ClientCredentialsTokenProvider, prometheus = prometheus
    )

    private fun query(request: TiltakspengerRequest): TiltakspengerResponse {
        val httpRequest = PostRequest(
            body = request, additionalHeaders = listOf(
                Header("Accept", "application/json")
            )
        )

        val response: TiltakspengerResponse? = client.post(uri = url, request = httpRequest)
        return requireNotNull(response)
    }

    override fun hentYtelseTiltakspenger(
        personidentifikatorer: String,
        fom: LocalDate,
        tom: LocalDate
    ): List<TiltakspengerPeriode> {
        return query(
            TiltakspengerRequest(
                personIdent = personidentifikatorer,
                fraOgMedDato = fom.toString(),
                tilOgMedDato = tom.toString()
            )
        ).perioder.map {
            TiltakspengerPeriode(
                fraOgMed = it.fraOgMedDato,
                tilOgMed = it.tilOgMedDato,
                kilde = it.kilde,
                tiltakspengerYtelseType = it.ytelseType
            )
        }
    }
}

internal data class TiltakspengerRequest(
    val personIdent: String,
    val fraOgMedDato: String,
    val tilOgMedDato: String
)

internal class TiltakspengerPeriodeResponse(
    val fraOgMedDato: LocalDate,
    val tilOgMedDato: LocalDate,
    val kilde: TiltakspengerKilde,
    val ytelseType: TiltakspengerYtelseType
)

internal class TiltakspengerResponse(
    val personIdent: String,
    val perioder: List<TiltakspengerPeriodeResponse>
)
