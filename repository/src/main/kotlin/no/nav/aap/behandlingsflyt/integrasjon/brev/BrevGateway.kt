package no.nav.aap.behandlingsflyt.integrasjon.brev

import no.nav.aap.behandlingsflyt.behandling.brev.BrevBehov
import no.nav.aap.behandlingsflyt.behandling.brev.GrunnlagBeregning
import no.nav.aap.behandlingsflyt.behandling.brev.Innvilgelse
import no.nav.aap.behandlingsflyt.behandling.brev.VurderesForUføretrygd
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingGateway
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingReferanse
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.HåndterConflictResponseHandler
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.TypeBrev
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.prometheus
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.brev.kontrakt.AvbrytBrevbestillingRequest
import no.nav.aap.brev.kontrakt.BestillBrevResponse
import no.nav.aap.brev.kontrakt.BestillBrevV2Request
import no.nav.aap.brev.kontrakt.Brev
import no.nav.aap.brev.kontrakt.BrevbestillingResponse
import no.nav.aap.brev.kontrakt.Brevtype
import no.nav.aap.brev.kontrakt.Faktagrunnlag
import no.nav.aap.brev.kontrakt.FerdigstillBrevRequest
import no.nav.aap.brev.kontrakt.ForhandsvisBrevRequest
import no.nav.aap.brev.kontrakt.HentSignaturerRequest
import no.nav.aap.brev.kontrakt.HentSignaturerResponse
import no.nav.aap.brev.kontrakt.KanDistribuereBrevReponse
import no.nav.aap.brev.kontrakt.KanDistribuereBrevRequest
import no.nav.aap.brev.kontrakt.MottakerDistStatus
import no.nav.aap.brev.kontrakt.MottakerDto
import no.nav.aap.brev.kontrakt.Signatur
import no.nav.aap.brev.kontrakt.SignaturGrunnlag
import no.nav.aap.brev.kontrakt.Språk
import no.nav.aap.brev.kontrakt.Vedlegg
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.gateway.Factory
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.Header
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.error.BadRequestHttpResponsException
import no.nav.aap.komponenter.httpklient.httpclient.post
import no.nav.aap.komponenter.httpklient.httpclient.put
import no.nav.aap.komponenter.httpklient.httpclient.request.GetRequest
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.request.PutRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import no.nav.aap.komponenter.json.DefaultJsonMapper
import org.slf4j.LoggerFactory
import java.io.InputStream
import java.net.URI
import kotlin.collections.orEmpty

class BrevGateway : BrevbestillingGateway {

    companion object : Factory<BrevbestillingGateway> {
        override fun konstruer(): BrevbestillingGateway {
            return BrevGateway()
        }
    }

    private val log = LoggerFactory.getLogger(javaClass)

    private val baseUri = URI.create(requiredConfigForKey("integrasjon.brev.url"))
    val config = ClientConfig(
        scope = requiredConfigForKey("integrasjon.brev.scope"),
    )

    private val client = RestClient(
        config = config,
        tokenProvider = ClientCredentialsTokenProvider,
        responseHandler = HåndterConflictResponseHandler(),
        prometheus = prometheus
    )

    override fun bestillBrevV2(
        saksnummer: Saksnummer,
        brukerIdent: Ident,
        behandlingReferanse: BehandlingReferanse,
        unikReferanse: String,
        brevBehov: BrevBehov,
        vedlegg: Vedlegg?,
        ferdigstillAutomatisk: Boolean,
    ): BrevbestillingReferanse {
        val request = BestillBrevV2Request(
            saksnummer = saksnummer.toString(),
            brukerIdent = brukerIdent.identifikator,
            behandlingReferanse = behandlingReferanse.referanse,
            brevtype = mapTypeBrev(brevBehov.typeBrev),
            unikReferanse = unikReferanse,
            sprak = Språk.NB, // TODO språk
            faktagrunnlag = mapFaktagrunnlag(brevBehov),
            ferdigstillAutomatisk = ferdigstillAutomatisk,
            vedlegg = vedlegg?.let { setOf(it) }.orEmpty()
        )
        val httpRequest = PostRequest(
            body = request,
            additionalHeaders = listOf(
                Header("Accept", "application/json")
            )
        )

        val url = baseUri.resolve("/api/v2/bestill")

        val response: BestillBrevResponse = requireNotNull(
            client.post(
                uri = url,
                request = httpRequest,
                mapper = { body, _ ->
                    DefaultJsonMapper.fromJson(body)
                })
        )

        return BrevbestillingReferanse(response.referanse)
    }

    override fun ferdigstill(
        referanse: BrevbestillingReferanse,
        signaturer: List<SignaturGrunnlag>,
        mottakere: List<MottakerDto>
    ): Boolean {
        val url = baseUri.resolve("/api/ferdigstill")

        val request = PostRequest(
            body = FerdigstillBrevRequest(referanse.brevbestillingReferanse, signaturer, mottakere),
            additionalHeaders = listOf(
                Header("Accept", "application/json")
            )
        )

        try {
            client.post<_, Unit>(
                uri = url,
                request = request
            )
        } catch (_: BadRequestHttpResponsException) {
            log.warn("Bad request i response for ferdigstilling av brev.")
            return false
        }

        return true
    }

    override fun hent(bestillingReferanse: BrevbestillingReferanse): BrevbestillingResponse {
        val url = baseUri.resolve("/api/bestilling/$bestillingReferanse")
        val request = GetRequest(
            additionalHeaders = listOf(
                Header("Accept", "application/json")
            )
        )
        val response: BrevbestillingResponse = requireNotNull(
            client.get(
                uri = url, request = request, mapper = { body, _ ->
                    DefaultJsonMapper.fromJson(body)
                })
        )

        return response
    }

    override fun oppdater(bestillingReferanse: BrevbestillingReferanse, brev: Brev) {
        val url = baseUri.resolve("/api/bestilling/$bestillingReferanse/oppdater")

        val request = PutRequest(body = brev)

        client.put<_, Unit>(url, request)
    }

    override fun forhåndsvis(
        bestillingReferanse: BrevbestillingReferanse,
        signaturer: List<SignaturGrunnlag>
    ): InputStream {

        val httpRequest = PostRequest(
            body = ForhandsvisBrevRequest(signaturer),
            additionalHeaders = listOf(
                Header("Accept", "application/json")
            )
        )

        val response: InputStream = requireNotNull(
            client.post(
                uri = baseUri.resolve("/api/bestilling/$bestillingReferanse/forhandsvis"),
                request = httpRequest,
                mapper = { body, _ ->
                    body
                })
        )
        return response
    }

    override fun avbryt(bestillingReferanse: BrevbestillingReferanse) {
        val url = baseUri.resolve("/api/avbryt")

        val request = PostRequest(
            body = AvbrytBrevbestillingRequest(bestillingReferanse.brevbestillingReferanse),
            additionalHeaders = listOf(
                Header("Accept", "application/json")
            )
        )
        client.post<_, Unit>(
            uri = url,
            request = request
        )
    }

    override fun hentSignaturForhåndsvisning(
        signaturer: List<SignaturGrunnlag>,
        brukerIdent: String,
        typeBrev: TypeBrev
    ): List<Signatur> {
        val httpRequest = PostRequest(
            body = HentSignaturerRequest(brukerIdent, mapTypeBrev(typeBrev), signaturer),
            additionalHeaders = listOf(
                Header("Accept", "application/json")
            )
        )

        val response: HentSignaturerResponse = requireNotNull(
            client.post(
                uri = baseUri.resolve("/api/forhandsvis-signaturer"),
                request = httpRequest,
                mapper = { body, _ ->
                    DefaultJsonMapper.fromJson(body)
                })
        )
        return response.signaturer
    }

    override fun kanDistribuereBrev(
        brukerIdent: String,
        mottakerIdentListe: List<String>
    ): List<MottakerDistStatus> {
        val httpRequest = PostRequest(
            body = KanDistribuereBrevRequest(brukerIdent = brukerIdent, mottakerIdentListe = mottakerIdentListe)
        )
        val response: KanDistribuereBrevReponse = requireNotNull(
            client.post(
                uri = baseUri.resolve("/api/distribusjon/kan-distribuere-brev"),
                request = httpRequest,
                mapper = { body, _ ->
                    DefaultJsonMapper.fromJson(body)
                })
        )
        return response.mottakereDistStatus
    }

    private fun mapTypeBrev(typeBrev: TypeBrev): Brevtype = when (typeBrev) {
        TypeBrev.VEDTAK_AVSLAG -> Brevtype.AVSLAG
        TypeBrev.VEDTAK_INNVILGELSE -> Brevtype.INNVILGELSE
        TypeBrev.VEDTAK_ENDRING -> Brevtype.VEDTAK_ENDRING
        TypeBrev.VARSEL_OM_BESTILLING -> Brevtype.VARSEL_OM_BESTILLING
        TypeBrev.FORHÅNDSVARSEL_BRUDD_AKTIVITETSPLIKT -> Brevtype.FORHÅNDSVARSEL_BRUDD_AKTIVITETSPLIKT
        TypeBrev.KLAGE_AVVIST -> Brevtype.KLAGE_AVVIST
        TypeBrev.KLAGE_OPPRETTHOLDELSE -> Brevtype.KLAGE_OPPRETTHOLDELSE
        TypeBrev.KLAGE_TRUKKET -> Brevtype.KLAGE_TRUKKET
        TypeBrev.FORHÅNDSVARSEL_KLAGE_FORMKRAV -> Brevtype.FORHÅNDSVARSEL_KLAGE_FORMKRAV
        TypeBrev.FORVALTNINGSMELDING -> Brevtype.FORVALTNINGSMELDING
        TypeBrev.VEDTAK_11_17 -> Brevtype.VEDTAK_11_17
        TypeBrev.VEDTAK_11_18 -> Brevtype.VEDTAK_11_18
        TypeBrev.VEDTAK_11_7 -> Brevtype.VEDTAK_11_7
        TypeBrev.VEDTAK_11_9 -> Brevtype.VEDTAK_11_9
    }

    private fun mapFaktagrunnlag(brevBehov: BrevBehov): Set<Faktagrunnlag> {
        return when (brevBehov) {
            is Innvilgelse ->
                buildSet {
                    add(Faktagrunnlag.AapFomDato(brevBehov.virkningstidspunkt))
                    if (brevBehov.tilkjentYtelse != null) {
                        add(
                            Faktagrunnlag.TilkjentYtelse(
                                dagsats = brevBehov.tilkjentYtelse?.dagsats?.verdi,
                                gradertDagsats = brevBehov.tilkjentYtelse?.gradertDagsats?.verdi,
                                barnetilleggSats = brevBehov.tilkjentYtelse?.barnetilleggsats?.verdi,
                                gradertBarnetillegg = brevBehov.tilkjentYtelse?.gradertBarnetillegg?.verdi,
                                gradertDagsatsInkludertBarnetillegg = brevBehov.tilkjentYtelse?.gradertDagsatsInkludertBarnetillegg?.verdi,
                                barnetillegg = brevBehov.tilkjentYtelse?.barnetillegg?.verdi,
                                antallBarn = brevBehov.tilkjentYtelse?.antallBarn,
                            )
                        )
                    }
                    if (brevBehov.grunnlagBeregning != null) {
                        add(
                            grunnlagBeregningTilFaktagrunnlag(brevBehov.grunnlagBeregning!!)                        )
                    }
                }

            is VurderesForUføretrygd -> {
                buildSet {
                    if (brevBehov.grunnlagBeregning != null) {
                        add(
                            grunnlagBeregningTilFaktagrunnlag(brevBehov.grunnlagBeregning!!)
                        )
                    }
                }
            }

            else -> emptySet()
        }
    }

    private fun grunnlagBeregningTilFaktagrunnlag(grunnlagBeregning: GrunnlagBeregning): Faktagrunnlag.GrunnlagBeregning {
        return Faktagrunnlag.GrunnlagBeregning(
            beregningstidspunkt = grunnlagBeregning.beregningstidspunkt,
            beregningsgrunnlag = grunnlagBeregning.beregningsgrunnlag?.verdi,
            inntekterPerÅr = grunnlagBeregning.inntekterPerÅr.map {
                Faktagrunnlag.GrunnlagBeregning.InntektPerÅr(it.år, it.inntekt)
            },
        )

    }
}
