package no.nav.aap.behandlingsflyt.integrasjon.kabal

import no.nav.aap.behandlingsflyt.behandling.klage.andreinstans.AndreinstansGateway
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
                brukersKlageMottattVedtaksinstans = kravDato, // TODO: Må hente kravdato. Dokument er ikke knyttet mot behandling, så undersøk hvordan dette skal gjøres. Må evt. legges på behandling
                hindreAutomatiskSvarbrev = false,
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
    val kildeReferanse: String
    // TODO: Legg til støtte for prosessfullmektig
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