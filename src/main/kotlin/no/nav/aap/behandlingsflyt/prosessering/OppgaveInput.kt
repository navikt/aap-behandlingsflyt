package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.behandling.Behandling
import no.nav.aap.behandlingsflyt.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sak.Sak
import no.nav.aap.behandlingsflyt.sak.SakId
import java.time.LocalDateTime

class OppgaveInput(val oppgave: Oppgave) {

    internal var id: Long? = null
    private var sakId: SakId? = null
    private var behandlingId: BehandlingId? = null
    private var nesteKjøring: LocalDateTime? = null

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

    fun forBehandling(sakId: SakId?, behandlingId: BehandlingId?): OppgaveInput {
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

    fun behandlingId(): BehandlingId {
        return behandlingId!!
    }

    fun behandlingIdOrNull(): BehandlingId? {
        return behandlingId
    }

    internal fun nesteKjøringTidspunkt(): LocalDateTime {
        if (nesteKjøring != null) {
            return nesteKjøring as LocalDateTime
        }
        return LocalDateTime.now()
    }

    fun type(): String = oppgave.type()

    override fun toString(): String {
        return "[${oppgave.type()}] - sakId = $sakId, behandlingId = $behandlingId"
    }

    fun medNesteKjøring(nesteKjøring: LocalDateTime): OppgaveInput {
        this.nesteKjøring = nesteKjøring
        return this
    }
}