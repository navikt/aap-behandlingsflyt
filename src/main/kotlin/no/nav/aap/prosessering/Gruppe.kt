package no.nav.aap.prosessering

import java.util.*

class Gruppe {

    private val oppgaver = LinkedList<OppgaveInput>()

    fun leggTil(oppgave: OppgaveInput): Gruppe {
        oppgaver.add(oppgave)

        return this
    }

    fun oppgaver() = oppgaver

    fun sakId() = oppgaver.map { it.sakId() }.toSet().single()
    fun behandlingId() = oppgaver.map { it.behandlingId() }.toSet().single()

    override fun toString(): String {
        return "Gruppe(oppgaver=$oppgaver)"
    }
}