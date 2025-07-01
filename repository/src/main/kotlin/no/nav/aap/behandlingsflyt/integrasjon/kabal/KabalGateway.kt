package no.nav.aap.behandlingsflyt.integrasjon.kabal

import no.nav.aap.behandlingsflyt.behandling.klage.andreinstans.AndreinstansGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.fullmektig.FullmektigVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.fullmektig.IdentType
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.resultat.KlageResultat
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.prometheus
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.gateway.Factory
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.Header
import no.nav.aap.komponenter.httpklient.httpclient.RestClient.Companion.withDefaultResponseHandler
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import org.slf4j.LoggerFactory
import java.net.URI
import java.time.LocalDate

class KabalGateway : AndreinstansGateway {
    companion object : Factory<AndreinstansGateway> {
        override fun konstruer(): AndreinstansGateway {
            return KabalGateway()
        }
    }

    private val log = LoggerFactory.getLogger(javaClass)

    private val baseUri = URI.create(requiredConfigForKey("integrasjon.kabal.url"))
    val config = ClientConfig(
        scope = requiredConfigForKey("integrasjon.kabal.scope"),
    )

    private val client = withDefaultResponseHandler(
        config = config,
        tokenProvider = ClientCredentialsTokenProvider,
        prometheus = prometheus
    )

    override fun oversendTilAndreinstans(
        saksnummer: Saksnummer, // TODO: Håndter Arena
        behandlingsreferanse: BehandlingReferanse,
        kravDato: LocalDate,
        klagenGjelder: Person,
        klageresultat: KlageResultat,
        saksbehandlersEnhet: String,
        kommentar: String,
        fullmektig: FullmektigVurdering?
    ) {
        val request = PostRequest(
            body = OversendtKlageAnkeV4(
                type = OversendtKlageAnkeV4Type.KLAGE,
                sakenGjelder = SakenGjelder(
                    id = OversendtPartId(
                        type = OversendtPartIdType.PERSON,
                        verdi = klagenGjelder.aktivIdent().identifikator
                    )
                ),
                fagsak = OversendtSak(
                    fagsakId = saksnummer.toString(),
                    fagsystem = Fagsystem.KELVIN
                ),
                hjemler = klageresultat.hjemlerSomSkalOpprettholdes().map { it.tilKabalHjemmel().name },
                ytelse = Ytelse.AAP_AAP,
                forrigeBehandlendeEnhet = saksbehandlersEnhet,
                tilknyttedeJournalposter = emptyList(), // TODO: Send med relevante journalposter?
                brukersKlageMottattVedtaksinstans = kravDato,
                hindreAutomatiskSvarbrev = false,
                prosessfullmektig = fullmektig?.tilOversendtProsessfullmektigV4(),
                kommentar = kommentar,
                kildeReferanse = behandlingsreferanse.referanse.toString()
            ),
            additionalHeaders = listOf(
                Header("Accept", "application/json")
            )
        )

        val url = baseUri.resolve("/api/oversendelse/v4/sak")

        log.info("Oversender klage til Kabal")
        client.post(uri = url, request = request, mapper = { _, _ -> })
    }
}

private fun FullmektigVurdering.tilOversendtProsessfullmektigV4(): OversendtProsessfullmektigV4? {
    if (!harFullmektig) {
        return null
    }
    return OversendtProsessfullmektigV4(
        id = fullmektigIdent?.let {
            OversendtPartId(
                type = it.type.tilOversendtPartIdType(),
                verdi = it.ident
            )
        },
        navn = fullmektigNavnOgAdresse?.navn,
        adresse = fullmektigNavnOgAdresse?.adresse?.let {
            OversendtAdresseV4(
                adresselinje1 = it.adresselinje1,
                adresselinje2 = it.adresselinje2,
                postnummer = it.postnummer,
                poststed = it.poststed,
                land = it.landkode
            )
        }
    )
}

data class OversendtKlageAnkeV4(
    val type: OversendtKlageAnkeV4Type,
    val sakenGjelder: SakenGjelder,
    val fagsak: OversendtSak,
    val hjemler: List<String>,
    val ytelse: Ytelse,
    val forrigeBehandlendeEnhet: String,
    val tilknyttedeJournalposter: List<String>,
    val brukersKlageMottattVedtaksinstans: LocalDate,
    val hindreAutomatiskSvarbrev: Boolean,
    val kildeReferanse: String,
    val kommentar: String = "",
    val prosessfullmektig: OversendtProsessfullmektigV4? = null
)

enum class OversendtKlageAnkeV4Type {
    KLAGE,
    ANKE
}

data class SakenGjelder(
    val id: OversendtPartId,
)

data class OversendtPartId(
    val type: OversendtPartIdType,
    val verdi: String,
)

enum class OversendtPartIdType {
    PERSON,
    VIRKSOMHET,
}

data class OversendtSak(
    val fagsakId: String? = null,
    val fagsystem: Fagsystem,
)

data class OversendtDokumentReferanse(
    val type: MottakDokumentType,
    val journalpostId: String,
)

data class OversendtProsessfullmektigV4(
    val id: OversendtPartId?,
    val navn: String?,
    val adresse: OversendtAdresseV4?,
)

data class OversendtAdresseV4(
    val adresselinje1: String?,
    val adresselinje2: String?,
    val postnummer: String?,
    val poststed: String?,
    val land: String,
)

internal fun IdentType.tilOversendtPartIdType(): OversendtPartIdType {
    return when (this) {
        IdentType.FNR_DNR -> OversendtPartIdType.PERSON
        IdentType.ORGNR, IdentType.UTL_ORGNR -> OversendtPartIdType.VIRKSOMHET
    }
}

enum class MottakDokumentType {
    BRUKERS_SOEKNAD,
    OPPRINNELIG_VEDTAK,
    BRUKERS_KLAGE,
    BRUKERS_ANKE,
    OVERSENDELSESBREV,
    KLAGE_VEDTAK,
    ANNET,
}

enum class Fagsystem {
    KELVIN,
    AO01 // Arena
}

enum class Ytelse { AAP_AAP }