package no.nav.aap.behandlingsflyt.integrasjon.arbeidsforhold

import no.nav.aap.behandlingsflyt.behandling.lovvalg.ArbeidINorgeGrunnlag
import no.nav.aap.behandlingsflyt.behandling.lovvalg.ArbeidAnsettelsesdetaljGrunnlag
import no.nav.aap.behandlingsflyt.behandling.lovvalg.Arbeidsforholdtype
import no.nav.aap.behandlingsflyt.behandling.lovvalg.Fartsomraade
import no.nav.aap.behandlingsflyt.behandling.lovvalg.Skipsregister
import no.nav.aap.behandlingsflyt.behandling.lovvalg.Skipstype
import no.nav.aap.behandlingsflyt.behandling.lovvalg.Yrke
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.aaregisteret.ArbeidsforholdGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.aaregisteret.ArbeidsforholdRequest
import no.nav.aap.behandlingsflyt.prometheus
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.gateway.Factory
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.Header
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.error.IkkeFunnetException
import no.nav.aap.komponenter.httpklient.httpclient.post
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.AzureM2MTokenProvider
import java.net.URI

class AARegisterGateway : ArbeidsforholdGateway {
    private val url =
        URI.create(requiredConfigForKey("INTEGRASJON_AAREG_URL") + "/api/v2/arbeidstaker/arbeidsforhold")
    private val config = ClientConfig(scope = requiredConfigForKey("INTEGRASJON_AAREG_SCOPE"))

    companion object : Factory<ArbeidsforholdGateway> {
        override fun konstruer(): ArbeidsforholdGateway {
            return AARegisterGateway()
        }
    }

    private val client = RestClient.withDefaultResponseHandler(
        config = config,
        tokenProvider = AzureM2MTokenProvider,
        prometheus = prometheus
    )

    private fun query(request: ArbeidsforholdRequest): List<ArbeidINorgeGrunnlag> {
        val httpRequest = PostRequest(
            body = request,
            additionalHeaders = listOf(
                Header("Accept", "application/json")
            )
        )
        val response: List<ArbeidsforholdResponse> = requireNotNull(client.post(uri = url, request = httpRequest))

        return response
            .filter { it.arbeidssted.type.uppercase() == "UNDERENHET" }
            .mapNotNull { arbeidsforhold ->
                val startdato = arbeidsforhold.ansettelsesperiode?.startdato ?: return@mapNotNull null
                ArbeidINorgeGrunnlag(
                    identifikator = arbeidsforhold.arbeidssted.identer.first().ident,
                    arbeidsforholdKode = Arbeidsforholdtype.fraKode(arbeidsforhold.type.kode),
                    startdato = startdato,
                    sluttdato = arbeidsforhold.ansettelsesperiode.sluttdato,
                    ansettelsesdetaljer = arbeidsforhold.ansettelsesdetaljer.map { detalj ->
                        ArbeidAnsettelsesdetaljGrunnlag(
                            skipsregister = detalj.skipsregister?.kode?.let { kode ->
                                Skipsregister.fraKode(kode)
                            },
                            skipstype = detalj.fartoeystype?.kode?.let { kode ->
                                Skipstype.fraKode(kode)
                            },
                            fartsomraade = detalj.fartsomraade?.kode?.let { kode ->
                                Fartsomraade.fraKode(kode)
                            },
                            yrke = detalj.yrke?.kode?.let { kode -> Yrke(kode, detalj.yrke.beskrivelse) },
                        )
                    }
                )
            }
    }

    override fun hentAARegisterData(request: ArbeidsforholdRequest): List<ArbeidINorgeGrunnlag> {
        return try {
            query(request)
        } catch (_: IkkeFunnetException) {
            // Fant ikke ident i AAreg, de returnerer 404
            emptyList()
        }
    }
}
