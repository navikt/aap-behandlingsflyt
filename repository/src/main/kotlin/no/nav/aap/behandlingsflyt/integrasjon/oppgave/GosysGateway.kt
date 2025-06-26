package no.nav.aap.behandlingsflyt.integrasjon.oppgave

import no.nav.aap.behandlingsflyt.behandling.klage.andreinstans.AndreinstansGateway
import no.nav.aap.behandlingsflyt.integrasjon.kabal.KabalGateway
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.gateway.Factory
import no.nav.aap.komponenter.gateway.Gateway
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.get
import no.nav.aap.komponenter.httpklient.httpclient.request.GetRequest

import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import no.nav.aap.verdityper.dokument.JournalpostId
import org.slf4j.LoggerFactory
import java.net.URI
import kotlin.jvm.javaClass

class GosysGateway : Gateway{

    companion object : Factory<Gateway> {
        override fun konstruer(): Gateway {
            return GosysGateway()
        }
    }

    private val log = LoggerFactory.getLogger(javaClass)
    private val baseUri = URI.create(requiredConfigForKey("integrasjon.gosys.url"))
    val config = ClientConfig(
        scope = requiredConfigForKey("integrasjon.gosys.scope"),
    )
    private val client = RestClient.withDefaultResponseHandler(
        config = config,
        tokenProvider = ClientCredentialsTokenProvider,
    )

    fun opprettOppgaveHvisIkkeEksisterer(oppgaveRequest: OpprettOppgaveRequest, bestillingReferanse: String) {
        val oppgaver = finnOppgaverForJournalpost(
            JournalpostId(oppgaveRequest.journalpostId),
            listOf(Oppgavetype.JOURNALFØRING, Oppgavetype.FORDELING),
            null,
            Statuskategori.AAPEN
        )

        if (oppgaver.isNotEmpty()) {
            log.info("Åpen oppgave for journalpost ${oppgaveRequest.journalpostId} finnes allerede - oppretter ingen ny")
            return
        }

        log.info("Oppretter oppave (${oppgaveRequest.oppgavetype}) for journalpost ${oppgaveRequest.journalpostId} i gosys")
        val url = baseUri.resolve("/api/bestilling/$bestillingReferanse/oppdater")
        val path = url.resolve("/api/v1/oppgaver")
        val request = PostRequest(oppgaveRequest)

        try {
            client.post(path, request) { _, _ -> }
        } catch (e: Exception) {
            log.warn("Feil mot oppgaveApi under opprettelse av oppgave: ${e.message}", e)
            throw e
        }

    }

    fun finnOppgaverForJournalpost(
        journalpostId: JournalpostId, oppgavetyper: List<Oppgavetype>, tema: String?, statuskategori: Statuskategori
    ): List<Long> {
        log.info("Finn oppgaver for journalpost: $journalpostId")
        val oppgaveparams = oppgavetyper.map { "&oppgavetype=${it.verdi}" }.joinToString(separator = "")
        val temaparams = if (tema != null) "&tema=$tema" else ""
        val path =
            baseUri.resolve("/api/v1/oppgaver?journalpostId=$journalpostId$oppgaveparams$temaparams&statuskategori=${statuskategori.name}")

        return client.get<FinnOppgaverResponse>(path, GetRequest())?.oppgaver?.map { it.id } ?: emptyList()
    }
}


data class Oppgave(
    val id: Long,
)

data class FinnOppgaverResponse(
    val oppgaver: List<Oppgave>
)

enum class Oppgavetype(val verdi: String) {
    JOURNALFØRING("JFR"),
    FORDELING("FDR")
}

enum class Statuskategori {
    AAPEN, AVSLUTTET
}