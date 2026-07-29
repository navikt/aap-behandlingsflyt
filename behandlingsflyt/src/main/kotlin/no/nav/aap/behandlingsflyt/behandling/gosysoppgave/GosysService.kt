package no.nav.aap.behandlingsflyt.behandling.gosysoppgave

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.refusjonkrav.NavKontorPeriodeDto
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.gateway.GatewayProvider
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class GosysService(private val gosysOppgaveGateway: GosysOppgaveGateway) {
    private val log = LoggerFactory.getLogger(javaClass)

    constructor(gatewayProvider: GatewayProvider) : this(
        gosysOppgaveGateway = gatewayProvider.provide()
    )

    fun opprettRefusjonskravOppgave(
        aktivIdent: Ident,
        bestillingReferanse: String,
        behandlingId: BehandlingId,
        navKontor: NavKontorPeriodeDto,
        skalFlereKontorerHaRefusjonskrav: Boolean
    ) {
        val beskrivelse = refusjonskravOppgaveBeskrivelse(behandlingId, navKontor, skalFlereKontorerHaRefusjonskrav)

        gosysOppgaveGateway.opprettOppgave(
            oppgavetype = OppgaveType.VURDER_KONSEKVENS_FOR_YTELSE,
            personIdent = aktivIdent,
            opprettetAvEnhetsnr = navKontor.enhetsNummer,
            tildeltEnhetsnr = navKontor.enhetsNummer,
            bestillingReferanse = bestillingReferanse,
            behandlingstema = Behandlingstema.REFUSJON,
            beskrivelse = beskrivelse,
            prioritet = Prioritet.HOY,
        )
    }

    private fun refusjonskravOppgaveBeskrivelse(
        behandlingId: BehandlingId,
        navKontor: NavKontorPeriodeDto,
        skalFlereKontorerHaRefusjonskrav: Boolean
    ): String {
        if (navKontor.vedtaksdato == null || navKontor.virkingsdato == null) {
            log.info(
                "Oppretter gosysoppgave med manglende dato for behandling $behandlingId, " +
                        "virkningsdato=${navKontor.virkingsdato}, vedtaksdato=${navKontor.vedtaksdato}"
            )
        }

        val beskrivelseSb = StringBuilder()
        beskrivelseSb.append(
            if (navKontor.virkingsdato != null && navKontor.vedtaksdato != null) {
                val fom = requireNotNull(navKontor.virkingsdato)
                val tom = requireNotNull(navKontor.vedtaksdato)
                "Refusjonskrav. Brukeren er innvilget etterbetaling av AAP fra ${
                    formatDateToSaksbehandlerVennlig(fom)
                } til ${
                    formatDateToSaksbehandlerVennlig(tom)
                }. Dere må sende refusjonskrav til NØS."
            } else {
                "Refusjonskrav. Brukeren er innvilget etterbetaling av AAP til ${navKontor.enhetsNummer}."
            }
        )

        beskrivelseSb.append(" Dersom dere ikke har refusjonskrav, må dere gi NØS beskjed om dette.")

        if (skalFlereKontorerHaRefusjonskrav) {
            beskrivelseSb.append(" Gi NØS beskjed om hvor mange Nav-kontor som har refusjonskrav.")
        }

        return beskrivelseSb.toString()
    }

    fun formatDateToSaksbehandlerVennlig(date: LocalDate): String {
        val outputFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
        return date.format(outputFormatter)
    }
}