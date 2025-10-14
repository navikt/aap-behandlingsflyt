package no.nav.aap.behandlingsflyt.integrasjon.gosys

import no.bekk.bekkopen.date.NorwegianDateUtil.addWorkingDaysToDate
import no.nav.aap.behandlingsflyt.behandling.gosysoppgave.OppgaveGateway
import no.nav.aap.behandlingsflyt.behandling.gosysoppgave.OpprettOppgaveRequest
import no.nav.aap.behandlingsflyt.behandling.gosysoppgave.Prioritet
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.refusjonkrav.NavKontorPeriodeDto
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
import java.time.format.DateTimeFormatter
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
        navKontor: NavKontorPeriodeDto
    ) {

        val beskrivelse =
            if (navKontor.fom != null && navKontor.tom != null) {
                val fom = requireNotNull(navKontor.fom)
                val tom = requireNotNull(navKontor.tom)
                "Refusjonskrav. Brukeren er innvilget etterbetaling av AAP fra ${
                    formatDateToSaksbehandlerVennlig(fom)
                } til ${
                    formatDateToSaksbehandlerVennlig(tom)
                }. Dere må sende refusjonskrav til NØS."
            } else {
                "Refusjonskrav. Brukeren er innvilget etterbetaling av AAP til ${navKontor.enhetsNummer}. Dere må sende refusjonskrav til NØS."
            }

        val oppgaveRequest = OpprettOppgaveRequest(
            oppgavetype = OppgaveType.VURDER_KONSEKVENS_FOR_YTELSE.verdi,
            tema = "AAP",
            prioritet = Prioritet.HOY,
            aktivDato = LocalDate.now().toString(),
            personident = aktivIdent.identifikator,
            tildeltEnhetsnr = navKontor.enhetsNummer,
            opprettetAvEnhetsnr = navKontor.enhetsNummer,
            beskrivelse = beskrivelse,
            behandlingstema = Behandlingstema.REFUSJON.kode,
            behandlingstype = null,
            fristFerdigstillelse = finnStandardOppgavefrist(),
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

fun formatDateToSaksbehandlerVennlig(date: LocalDate): String {
    val outputFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    return date.format(outputFormatter)
}

enum class OppgaveType(val verdi: String) {
    VURDER_KONSEKVENS_FOR_YTELSE("VUR_KONS_YTE"),
}
