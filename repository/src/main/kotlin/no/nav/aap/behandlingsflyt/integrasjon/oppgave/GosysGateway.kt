package no.nav.aap.behandlingsflyt.integrasjon.oppgave

import no.nav.aap.behandlingsflyt.behandling.gosysoppgave.OppgaveGateway
import no.nav.aap.behandlingsflyt.behandling.gosysoppgave.OpprettOppgaveRequest
import no.nav.aap.behandlingsflyt.behandling.gosysoppgave.Prioritet
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
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
import java.time.LocalDate
import kotlin.jvm.javaClass
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident

class GosysGateway : OppgaveGateway {

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

    override fun opprettOppgaveHvisIkkeEksisterer(aktivIdent: Ident, bestillingReferanse: String, behandlingId: BehandlingId) {

        val oppgaveRequest = OpprettOppgaveRequest(
            oppgavetype = OppgaveType.JOURNALFØRING.verdi,
            tema = "AAP",
            prioritet = Prioritet.NORM,
            aktivDato = LocalDate.now().toString(),
            personident = aktivIdent.toString(),
            orgnr = null,
            tildeltEnhetsnr = null,
            opprettetAvEnhetsnr = null,
            journalpostId = "1",
            tilordnetRessurs = null,
            beskrivelse = "Krav om refusjon av sosialhjelp for bruker av AAP",
            behandlingstema = "AAP",
            behandlingstype = "AAP",
            fristFerdigstillelse = LocalDate.now()
        )

        val oppgaver = finnOppgaverForJournalpost(
            JournalpostId(oppgaveRequest.journalpostId),
            listOf(OppgaveType.JOURNALFØRING, OppgaveType.FORDELING),
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
        journalpostId: JournalpostId, oppgavetyper: List<OppgaveType>, tema: String?, statuskategori: Statuskategori
    ): List<Long> {
        log.info("Finn oppgaver for journalpost: $journalpostId")
        val oppgaveparams = oppgavetyper.joinToString(separator = "") { "&oppgavetype=${it.verdi}" }
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

enum class OppgaveType(val verdi: String) {
    JOURNALFØRING("JFR"),
    FORDELING("FDR")
}

enum class Statuskategori {
    AAPEN, AVSLUTTET
}