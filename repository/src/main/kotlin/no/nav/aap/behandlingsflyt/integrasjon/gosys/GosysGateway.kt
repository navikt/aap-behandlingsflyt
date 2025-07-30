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


import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
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

    override fun opprettOppgave(aktivIdent: Ident, bestillingReferanse: String, behandlingId: BehandlingId, navKontor: String) {

         val oppgaveRequest = OpprettOppgaveRequest(
            oppgavetype = OppgaveType.FORDELING.verdi,
            tema = "AAP",
            prioritet = Prioritet.NORM,
            aktivDato = LocalDate.now().toString(),
            personident = aktivIdent.toString(),
            tildeltEnhetsnr = navKontor,
            beskrivelse = "Krav om refusjon av sosialhjelp for bruker av AAP",
            behandlingstema = "AAP",
            behandlingstype = "AAP",
            fristFerdigstillelse = LocalDate.now().plusDays(7),
        )

        val path = baseUri.resolve("/api/v1/oppgaver")
        val request = PostRequest(oppgaveRequest)

        log.info("Kaller Gosysoppgave på path: $path")
        try {
            client.post(path, request) { _, _ -> }
        } catch (e: Exception) {
            log.warn("Feil mot oppgaveApi under opprettelse av oppgave: ${e.message}", e)
            throw e
        }

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