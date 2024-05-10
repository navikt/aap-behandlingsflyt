package no.nav.aap.motor

import no.nav.aap.motor.retry.RekjørFeiledeOppgaver

object OppgaveType {
    private val oppgaver = HashMap<String, Oppgave>()

    init {
        oppgaver[RekjørFeiledeOppgaver.type()] = RekjørFeiledeOppgaver
    }

    fun leggTil(oppgave: Oppgave) {
        oppgaver[oppgave.type()] = oppgave
    }

    fun parse(type: String): Oppgave {
        return oppgaver.getValue(type)
    }

    fun cronTypes(): List<String> {
        return oppgaver.filter { it.value.cron() != null }.keys.toList()
    }

}
