package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.domene.behandling.Behandling
import no.nav.aap.behandlingsflyt.sak.Sak

private const val SAK_ID = "sakId"
private const val BEHANDLING_ID = "behandlingId"

class OppgaveInput(private val parameter: HashMap<String, String> = HashMap(), val oppgave: Oppgave) {

    fun forSak(sak: Sak): OppgaveInput {
        parameter[SAK_ID] = sak.id.toString()

        return this
    }

    fun forBehandling(behandling: Behandling): OppgaveInput {
        parameter[SAK_ID] = behandling.sakId.toString()
        parameter[BEHANDLING_ID] = behandling.sakId.toString()

        return this
    }

    fun forBehandling(sakId: Long, behandlingId: Long): OppgaveInput {
        parameter[SAK_ID] = sakId.toString()
        parameter[BEHANDLING_ID] = behandlingId.toString()

        return this
    }

    fun sakId() = parameter.getValue(SAK_ID).toLong()
    fun behandlingId() = parameter.getValue(BEHANDLING_ID).toLong()
    fun type(): String = oppgave.type()

    override fun toString(): String {
        return "[${oppgave.type()}]"
    }
}