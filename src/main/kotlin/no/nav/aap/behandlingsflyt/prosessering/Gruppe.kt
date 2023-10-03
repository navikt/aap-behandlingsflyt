package no.nav.aap.behandlingsflyt.prosessering

import java.util.*

class Gruppe {

    private val oppgaver = LinkedList<OppgaveInput>()

    fun leggTil(oppgave: OppgaveInput): Gruppe {
        oppgaver.add(oppgave)

        return this
    }

    fun oppgaver() = oppgaver

    fun sakId() = oppgaver.map { it.sakId() }.toSet().firstOrNull()
    fun behandlingId() = oppgaver.map { it.behandlingId() }.toSet().firstOrNull()

    override fun toString(): String {
        return "Gruppe(sak=${sakId()}, behandling=${behandlingId()}, oppgaver=$oppgaver)"
    }
}