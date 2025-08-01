package no.nav.aap.behandlingsflyt.integrasjon.oppgave


import no.nav.aap.behandlingsflyt.behandling.gosysoppgave.OppgaveGateway
import no.nav.aap.behandlingsflyt.behandling.gosysoppgave.OpprettOppgaveRequest
import no.nav.aap.behandlingsflyt.behandling.gosysoppgave.Prioritet
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.gateway.Factory
import no.nav.aap.komponenter.gateway.Gateway
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import org.slf4j.LoggerFactory
import java.net.URI
import java.time.LocalDate

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

    override fun opprettOppgave(
        aktivIdent: Ident,
        bestillingReferanse: String,
        behandlingId: BehandlingId,
        navKontor: String
    ) {

        val oppgaveRequest = OpprettOppgaveRequest(
            oppgavetype = OppgaveType.FORDELING.verdi,
            tema = "AAP",
            prioritet = Prioritet.NORM,
            aktivDato = LocalDate.now().toString(),
            personident = aktivIdent.identifikator,
            tildeltEnhetsnr = navKontor,
            opprettetAvEnhetsnr = navKontor,
            beskrivelse = "Krav om refusjon av sosialhjelp for bruker av AAP",
            behandlingstema = Behandlingstema.AAP.kode,
            behandlingstype = "AAP",
            fristFerdigstillelse = LocalDate.now().plusDays(7),
        )

        val path = baseUri.resolve("/api/v1/oppgaver")
        val request = PostRequest(oppgaveRequest)
        // Midlertidig logging for debugging i test
        log.info("Kaller Gosysoppgave med request: ${oppgaveRequest}")

        try {
            client.post(path, request) { _, _ -> }
        } catch (e: Exception) {
            log.warn("Feil mot oppgaveApi under opprettelse av oppgave: ${e.message}", e)
            throw e
        }

    }


    enum class Behandlingstema(val kode: String) {
        AAP("ab0014");
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