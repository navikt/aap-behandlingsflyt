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
import no.nav.aap.komponenter.miljo.Miljø
import org.slf4j.LoggerFactory
import java.net.URI

class AARegisterGateway : ArbeidsforholdGateway {
    private val log = LoggerFactory.getLogger(javaClass)
    private val url =
        URI.create(requiredConfigForKey("integrasjon.aareg.url") + "/api/v2/arbeidstaker/arbeidsforholdoversikt")
    private val config = ClientConfig(scope = requiredConfigForKey("integrasjon.aareg.scope"))

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
        val response: ArbeidsforholdoversiktResponse = requireNotNull(client.post(uri = url, request = httpRequest))

        if (Miljø.erDev()) {
            log.info("AARegister response: $response")
        }

        return response.arbeidsforholdoversikter
            .filter { it.arbeidssted.type.uppercase() == "UNDERENHET" }
            .map { arbeidsforhold ->
                ArbeidINorgeGrunnlag(
                    identifikator = arbeidsforhold.arbeidssted.identer.first().ident,
                    arbeidsforholdKode = Arbeidsforholdtype.fraKode(arbeidsforhold.type.kode),
                    startdato = arbeidsforhold.startdato,
                    sluttdato = arbeidsforhold.sluttdato,
                    yrke = arbeidsforhold.yrke?.kode?.let { kode -> Yrke(kode, arbeidsforhold.yrke.beskrivelse) },
                    ansettelsesdetaljer = arbeidsforhold.ansettelsesdetaljer.map { detalj ->
                        ArbeidAnsettelsesdetaljGrunnlag(
                            skipsregister = detalj.skipsregister?.kode?.let { kode ->
                                Skipsregister(
                                    kode,
                                    detalj.skipsregister.beskrivelse
                                )
                            },
                            skipstype = detalj.fartoeystype?.kode?.let { kode ->
                                Skipstype(
                                    kode,
                                    detalj.fartoeystype.beskrivelse
                                )
                            },
                            fartsomraade = detalj.fartsomraade?.kode?.let { kode ->
                                Fartsomraade(
                                    kode,
                                    detalj.fartsomraade.beskrivelse
                                )
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
