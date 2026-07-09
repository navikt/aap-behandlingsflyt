package no.nav.aap.behandlingsflyt.behandling.gosysoppgave

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.refusjonkrav.NavKontorPeriodeDto
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class GosysServiceTest {

    private val fakeGateway = FakeGosysOppgaveGateway()
    private val service = GosysService(fakeGateway)

    @BeforeEach
    fun setUp() {
        fakeGateway.opprettedeOppgaver.clear()
    }

    @Test
    fun `beskrivelse med begge datoer og enkelt kontor inneholder periode og mangler flerekontor-setning`() {
        service.opprettRefusjonskravOppgave(
            aktivIdent = Ident("12345678901"),
            bestillingReferanse = "ref-1",
            behandlingId = BehandlingId(1L),
            navKontor = navKontorMedDatoer(
                fom = LocalDate.of(2024, 1, 1),
                tom = LocalDate.of(2024, 12, 31),
                enhet = "1234"
            ),
            skalFlereKontorerHaRefusjonskrav = false,
        )

        val beskrivelse = fakeGateway.opprettedeOppgaver.single().beskrivelse

        assertEquals(
            "Refusjonskrav. Brukeren er innvilget etterbetaling av AAP fra 01.01.2024 til 31.12.2024. Dere må sende refusjonskrav til NØS. Dersom dere ikke har refusjonskrav, må dere gi NØS beskjed om dette.",
            beskrivelse
        )
    }

    @Test
    fun `beskrivelse med begge datoer og flere kontorer inneholder periode og flerekontor-setning`() {
        service.opprettRefusjonskravOppgave(
            aktivIdent = Ident("12345678901"),
            bestillingReferanse = "ref-2",
            behandlingId = BehandlingId(2L),
            navKontor = navKontorMedDatoer(
                fom = LocalDate.of(2024, 1, 1),
                tom = LocalDate.of(2024, 12, 31),
                enhet = "1234"
            ),
            skalFlereKontorerHaRefusjonskrav = true,
        )

        val beskrivelse = fakeGateway.opprettedeOppgaver.single().beskrivelse

        assertEquals(
            "Refusjonskrav. Brukeren er innvilget etterbetaling av AAP fra 01.01.2024 til 31.12.2024. Dere må sende refusjonskrav til NØS. Dersom dere ikke har refusjonskrav, må dere gi NØS beskjed om dette. Gi NØS beskjed om hvor mange Nav-kontor som har refusjonskrav.",
            beskrivelse
        )
    }

    @Test
    fun `beskrivelse uten datoer og enkelt kontor bruker enhetnummer som fallback og mangler flerekontor-setning`() {
        service.opprettRefusjonskravOppgave(
            aktivIdent = Ident("12345678901"),
            bestillingReferanse = "ref-3",
            behandlingId = BehandlingId(3L),
            navKontor = navKontorUtenDatoer(enhet = "1234"),
            skalFlereKontorerHaRefusjonskrav = false,
        )

        val beskrivelse = fakeGateway.opprettedeOppgaver.single().beskrivelse

        assertEquals(
            "Refusjonskrav. Brukeren er innvilget etterbetaling av AAP til 1234. Dersom dere ikke har refusjonskrav, må dere gi NØS beskjed om dette.",
            beskrivelse
        )
    }

    @Test
    fun `beskrivelse uten datoer og flere kontorer bruker enhetnummer som fallback og inneholder flerekontor-setning`() {
        service.opprettRefusjonskravOppgave(
            aktivIdent = Ident("12345678901"),
            bestillingReferanse = "ref-4",
            behandlingId = BehandlingId(4L),
            navKontor = navKontorUtenDatoer(enhet = "1234"),
            skalFlereKontorerHaRefusjonskrav = true,
        )

        val beskrivelse = fakeGateway.opprettedeOppgaver.single().beskrivelse

        assertEquals(
            "Refusjonskrav. Brukeren er innvilget etterbetaling av AAP til 1234. Dersom dere ikke har refusjonskrav, må dere gi NØS beskjed om dette. Gi NØS beskjed om hvor mange Nav-kontor som har refusjonskrav.",
            beskrivelse
        )
    }

    private fun navKontorMedDatoer(fom: LocalDate, tom: LocalDate, enhet: String) =
        NavKontorPeriodeDto(enhetsNummer = enhet, virkingsdato = fom, vedtaksdato = tom)

    private fun navKontorUtenDatoer(enhet: String) =
        NavKontorPeriodeDto(enhetsNummer = enhet, virkingsdato = null, vedtaksdato = null)
}

private class FakeGosysOppgaveGateway : GosysOppgaveGateway {
    data class OppgaveKall(val beskrivelse: String)

    val opprettedeOppgaver = mutableListOf<OppgaveKall>()

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
        fristFerdigstillelse: LocalDate?,
    ) {
        opprettedeOppgaver.add(OppgaveKall(beskrivelse = beskrivelse))
    }
}