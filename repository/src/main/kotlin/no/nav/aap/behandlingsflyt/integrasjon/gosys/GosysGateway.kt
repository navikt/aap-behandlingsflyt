package no.nav.aap.behandlingsflyt.integrasjon.gosys

import no.bekk.bekkopen.date.NorwegianDateUtil.addWorkingDaysToDate
import no.nav.aap.behandlingsflyt.behandling.gosysoppgave.Behandlingstema
import no.nav.aap.behandlingsflyt.behandling.gosysoppgave.GosysOppgaveGateway
import no.nav.aap.behandlingsflyt.behandling.gosysoppgave.OppgaveType
import no.nav.aap.behandlingsflyt.behandling.gosysoppgave.OpprettOppgaveRequest
import no.nav.aap.behandlingsflyt.behandling.gosysoppgave.Prioritet
import no.nav.aap.behandlingsflyt.prometheus
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.gateway.Factory
import no.nav.aap.komponenter.gateway.Gateway
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.AzureM2MTokenProvider
import org.slf4j.LoggerFactory
import java.net.URI
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalDateTime.now
import java.time.ZoneId.systemDefault
import java.util.*

class GosysGateway : GosysOppgaveGateway {

    companion object : Factory<Gateway> {
        override fun konstruer(): Gateway {
            return GosysGateway()
        }
    }

    private val log = LoggerFactory.getLogger(javaClass)
    private val baseUri = URI.create(requiredConfigForKey("INTEGRASJON_GOSYS_URL"))
    val config = ClientConfig(
        scope = requiredConfigForKey("INTEGRASJON_GOSYS_SCOPE"),
    )
    private val client = RestClient.withDefaultResponseHandler(
        config = config,
        tokenProvider = AzureM2MTokenProvider,
        prometheus = prometheus
    )

    override fun opprettOppgave(
        oppgavetype: OppgaveType,
        tema: String,
        personIdent: Ident,
        bestillingReferanse: String,
        tildeltEnhetsnr: String,
        opprettetAvEnhetsnr: String,
        behandlingstema: Behandlingstema,
        beskrivelse: String,
        prioritet: Prioritet,
        aktivDato: LocalDate,
        fristFerdigstillelse: LocalDate?
    ) {
        val oppgaveRequest = OpprettOppgaveRequest(
            oppgavetype = oppgavetype.verdi,
            tema = tema,
            prioritet = prioritet,
            aktivDato = aktivDato.toString(),
            personident = personIdent.identifikator,
            tildeltEnhetsnr = tildeltEnhetsnr,
            opprettetAvEnhetsnr = opprettetAvEnhetsnr,
            beskrivelse = beskrivelse,
            behandlingstema = behandlingstema.kode,
            behandlingstype = null,
            fristFerdigstillelse = fristFerdigstillelse ?: finnStandardOppgavefrist()
        )

        val path = baseUri.resolve("/api/v1/oppgaver")
        val request = PostRequest(oppgaveRequest)
        try {
            client.post(path, request) { _, _ -> }
            log.info("Opprettet refusjonsoppgave mot Gosys: $oppgaveRequest ")
        } catch (e: Exception) {
            log.error("Feil mot oppgaveApi under opprettelse av oppgave: ${e.message}", e)
            throw e
        }

    }
}

fun finnStandardOppgavefrist(nå: LocalDateTime = now()): LocalDate {
    val SISTE_ARBEIDSTIME = 12
    fun Int.dagerTilFrist() = if (this < SISTE_ARBEIDSTIME) 1 else 2
    return with(nå)
    {
        addWorkingDaysToDate(
            Date.from(toLocalDate().atStartOfDay(systemDefault()).toInstant()),
            hour.dagerTilFrist()
        ).toInstant()
            .atZone(systemDefault()).toLocalDate()
    }
}

