package no.nav.aap.behandlingsflyt.integrasjon.oppgave

import no.bekk.bekkopen.date.NorwegianDateUtil.addWorkingDaysToDate
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
import java.time.LocalDateTime
import java.time.LocalDateTime.now
import java.time.ZoneId.systemDefault
import java.util.Date

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
            oppgavetype = OppgaveType.VVURDER_KONSEKVENS_FOR_YTELSE.verdi,
            tema = "AAP",
            prioritet = Prioritet.HOY,
            aktivDato = LocalDate.now().toString(),
            personident = aktivIdent.identifikator,
            tildeltEnhetsnr = navKontor,
            opprettetAvEnhetsnr = navKontor,
            beskrivelse = "Refusjonskrav. Brukeren er innvilget etterbetaling av (ytelse) fra (dato) til (dato). Dere må sende refusjonskrav til NØS.",
            behandlingstema = Behandlingstema.REFUSJON.kode,
            behandlingstype = null,
            fristFerdigstillelse = finnStandardOppgavefrist(),
        )

        val path = baseUri.resolve("/api/v1/oppgaver")
        val request = PostRequest(oppgaveRequest)
        // Midlertidig logging for debugging i test
        log.info("Kaller Gosysoppgave med request: ${oppgaveRequest}")
        log.info("Kaller Gosysoppgave med rå request: ${request.body()}")
        try {
            client.post(path, request) { _, _ -> }
        } catch (e: Exception) {
            log.error("Feil mot oppgaveApi under opprettelse av oppgave: ${e.message}", e)
            throw e
        }

    }


    enum class Behandlingstema(val kode: String) {
        REFUSJON("ab0504");
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

data class Oppgave(
    val id: Long,
)

data class FinnOppgaverResponse(
    val oppgaver: List<Oppgave>
)

enum class OppgaveType(val verdi: String) {
    VVURDER_KONSEKVENS_FOR_YTELSE("VUR_KONS_YTE"),
}

enum class Statuskategori {
    AAPEN, AVSLUTTET
}