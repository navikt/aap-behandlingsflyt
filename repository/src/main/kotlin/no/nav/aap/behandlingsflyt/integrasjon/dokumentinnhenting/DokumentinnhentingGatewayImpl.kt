package no.nav.aap.behandlingsflyt.integrasjon.dokumentinnhenting

import no.nav.aap.behandlingsflyt.behandling.behandlerdialog.BegrensetDokumentInfoDto
import no.nav.aap.behandlingsflyt.behandling.behandlerdialog.DokumenterForJournalpostParameter
import no.nav.aap.behandlingsflyt.behandling.dialogmelding.FellesDialogmeldingDto
import no.nav.aap.behandlingsflyt.behandling.dialogmelding.HentDialogmeldingerForSakParams
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.dokumentinnhenting.DokumentinnhentingGateway
import no.nav.aap.behandlingsflyt.prometheus
import no.nav.aap.dokumentinnhenting.kontrakt.BehandlingsflytToDokumentInnhentingBestillingDto
import no.nav.aap.dokumentinnhenting.kontrakt.DialogmeldingForhåndsvisningDto
import no.nav.aap.dokumentinnhenting.kontrakt.DialogmeldingStatusTilBehandslingsflytDto
import no.nav.aap.dokumentinnhenting.kontrakt.FastlegeDto
import no.nav.aap.dokumentinnhenting.kontrakt.ForhåndsvisDialogmeldingDto
import no.nav.aap.dokumentinnhenting.kontrakt.HentFastlegeDto
import no.nav.aap.dokumentinnhenting.kontrakt.PåminnelseDto
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.gateway.Factory
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.Header
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.post
import no.nav.aap.komponenter.httpklient.httpclient.request.GetRequest
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.AzureM2MTokenProvider
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.AzureOBOTokenProvider
import no.nav.aap.komponenter.json.DefaultJsonMapper
import java.net.URI

/**
 * Bestiller dokumenter fra dokumentinnhenting
 */
class DokumentinnhentingGatewayImpl : DokumentinnhentingGateway {
    private val syfoUri = requiredConfigForKey("INTEGRASJON_DOKUMENTINNHENTING_URL") + "/syfo"
    private val config = ClientConfig(scope = requiredConfigForKey("INTEGRASJON_DOKUMENTINNHENTING_SCOPE"))
    private val påminnelseUri =
        requiredConfigForKey("INTEGRASJON_DOKUMENTINNHENTING_URL") + "/dialogmelding/paaminnelse"
    private val dialogmeldingUri = requiredConfigForKey("INTEGRASJON_DOKUMENTINNHENTING_URL") + "/dialogmelding"
    private val dokumenterUri = requiredConfigForKey("INTEGRASJON_DOKUMENTINNHENTING_URL") + ""

    private val client = RestClient.withDefaultResponseHandler(
        config = config,
        tokenProvider = AzureM2MTokenProvider,
        prometheus = prometheus
    )

    private val oboClient = RestClient.withDefaultResponseHandler(
        config = config,
        tokenProvider = AzureOBOTokenProvider,
        prometheus = prometheus
    )

    companion object : Factory<DokumentinnhentingGateway> {
        override fun konstruer(): DokumentinnhentingGateway {
            return DokumentinnhentingGatewayImpl()
        }
    }

    override fun bestillLegeerklæring(request: BehandlingsflytToDokumentInnhentingBestillingDto): String {
        val request = PostRequest(
            body = request,
            additionalHeaders = listOf(
                Header("Nav-Consumer-Id", "aap-behandlingsflyt"),
                Header("Accept", "application/json")
            )
        )

        return requireNotNull(client.post(uri = URI.create("$syfoUri/dialogmeldingbestilling"), request))
    }

    override fun sendPåminnelseForBestilling(påminnelseRequest: PåminnelseDto): String {
        val request = PostRequest(
            body = påminnelseRequest,
            additionalHeaders = listOf(
                Header("Nav-Consumer-Id", "aap-behandlingsflyt"),
                Header("Accept", "application/json")
            )
        )
        return requireNotNull(client.post(uri = URI.create("$påminnelseUri/send"), request))
    }

    override fun avbrytAutomatiskPåminnelseForBestilling(påminnelseRequest: PåminnelseDto) {
        val request = PostRequest(
            body = påminnelseRequest,
            additionalHeaders = listOf(
                Header("Nav-Consumer-Id", "aap-behandlingsflyt"),
                Header("Accept", "application/json")
            )
        )
        client.post(uri = URI.create("$påminnelseUri/avbryt-automatisk-paaminnelse"), request, mapper = { _, _ -> })
    }

    override fun gjenopptaAutomatiskPåminnelseForBestilling(påminnelseRequest: PåminnelseDto) {
        val request = PostRequest(
            body = påminnelseRequest,
            additionalHeaders = listOf(
                Header("Nav-Consumer-Id", "aap-behandlingsflyt"),
                Header("Accept", "application/json")
            )
        )
        client.post(uri = URI.create("$påminnelseUri/gjenoppta-automatisk-paaminnelse"), request, mapper = { _, _ -> })
    }

    override fun legeerklæringStatus(saksnummer: String): List<DialogmeldingStatusTilBehandslingsflytDto> {
        val request = GetRequest(
            additionalHeaders = listOf(
                Header("Nav-Consumer-Id", "aap-behandlingsflyt"),
                Header("Accept", "application/json")
            )
        )

        return requireNotNull(
            client.get(
                uri = URI.create("$syfoUri/status/$saksnummer"),
                request = request,
                mapper = { body, _ -> DefaultJsonMapper.fromJson(body) })
        )
    }

    override fun forhåndsvisDialogmelding(request: ForhåndsvisDialogmeldingDto): DialogmeldingForhåndsvisningDto {
        val request = PostRequest(
            body = request,
            additionalHeaders = listOf(
                Header("Nav-Consumer-Id", "aap-behandlingsflyt"),
                Header("Accept", "application/json")
            )
        )

        return requireNotNull(client.post(uri = URI.create("$syfoUri/brevpreview"), request))
    }

    override fun hentDialogmeldingerForSak(request: HentDialogmeldingerForSakParams): List<FellesDialogmeldingDto> {
        val saksnummer = request.saksnummer
        val request = GetRequest(
            additionalHeaders = listOf(
                Header("Nav-Consumer-Id", "aap-behandlingsflyt"),
                Header("Accept", "application/json")
            )
        )

        return requireNotNull(
            client.get(
                uri = URI.create("$dialogmeldingUri/$saksnummer/dialogmeldinger"),
                request = request,
                mapper = { body, _ -> DefaultJsonMapper.fromJson(body) }
            )
        )
    }

    override fun hentDokumentoversiktForJournalpost(request: DokumenterForJournalpostParameter): List<BegrensetDokumentInfoDto> {
        val journalpostId = request.journalpostId
        val request = GetRequest(
            additionalHeaders = listOf(
                Header("Nav-Consumer-Id", "aap-behandlingsflyt"),
                Header("Accept", "application/json")
            )
        )

        return requireNotNull(
            client.get(
                uri = URI.create("$dokumenterUri/api/dokumenter/$journalpostId/dokumentliste"),
                request = request,
                mapper = { body, _ -> DefaultJsonMapper.fromJson(body) }
            )
        )
    }

    override fun hentFastlege(request: HentFastlegeDto, currentToken: OidcToken): FastlegeDto {
        val request = PostRequest(
            body = request,
            additionalHeaders = listOf(
                Header("Nav-Consumer-Id", "aap-behandlingsflyt"),
                Header("Accept", "application/json")
            ),
            currentToken = currentToken,
        )

        return requireNotNull(oboClient.post(uri = URI.create("$syfoUri/behandleroppslag/fastlege"), request))
    }
}