package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.behandling.Behandling
import no.nav.aap.behandlingsflyt.sak.Sak
import no.nav.aap.behandlingsflyt.sak.SakId

class OppgaveInput(val oppgave: Oppgave) {

    internal var id: Long? = null
    private var sakId: SakId? = null
    private var behandlingId: Long? = null

    internal fun medId(id: Long): OppgaveInput {
        this.id = id
        return this
    }

    fun forSak(sak: Sak): OppgaveInput {
        sakId = sak.id

        return this
    }

    fun forBehandling(behandling: Behandling): OppgaveInput {
        sakId = behandling.sakId
        behandlingId = behandling.id

        return this
    }

    fun forBehandling(sakId: SakId?, behandlingId: Long?): OppgaveInput {
        this.sakId = sakId
        this.behandlingId = behandlingId

        return this
    }

    fun sakIdOrNull(): SakId? {
        return sakId
    }

    fun sakId(): SakId {
        return sakId!!
    }

    fun behandlingId(): Long {
        return behandlingId!!
    }

    fun behandlingIdOrNull(): Long? {
        return behandlingId
    }

    fun type(): String = oppgave.type()

    override fun toString(): String {
        return "[${oppgave.type()}]"
    }
}