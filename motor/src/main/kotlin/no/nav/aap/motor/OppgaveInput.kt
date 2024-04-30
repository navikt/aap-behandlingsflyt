package no.nav.aap.motor

import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import no.nav.aap.verdityper.sakogbehandling.SakId
import java.time.LocalDateTime

class OppgaveInput(val oppgave: Oppgave) {

    internal var id: Long? = null
    private var sakId: SakId? = null
    private var behandlingId: BehandlingId? = null
    private var nesteKjøring: LocalDateTime? = null
    private var antallFeil: Long = 0
    private var status: OppgaveStatus = OppgaveStatus.KLAR

    internal fun medId(id: Long): OppgaveInput {
        this.id = id
        return this
    }

    internal fun medStatus(status: OppgaveStatus): OppgaveInput {
        this.status = status
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

    fun status(): OppgaveStatus {
        return status
    }

    fun behandlingId(): BehandlingId {
        return behandlingId!!
    }

    fun behandlingIdOrNull(): BehandlingId? {
        return behandlingId
    }

    fun medAntallFeil(antallFeil: Long): OppgaveInput {
        this.antallFeil = antallFeil
        return this
    }

    internal fun nesteKjøringTidspunkt(): LocalDateTime {
        if (nesteKjøring != null) {
            return nesteKjøring as LocalDateTime
        }
        nesteKjøring = LocalDateTime.now()
        return LocalDateTime.now()
    }

    fun type(): String {
        return oppgave.type()
    }

    override fun toString(): String {
        return "[${oppgave.type()}] - id = $id, sakId = $sakId, behandlingId = $behandlingId"
    }

    fun medNesteKjøring(nesteKjøring: LocalDateTime): OppgaveInput {
        this.nesteKjøring = nesteKjøring
        return this
    }

    fun skalMarkeresSomFeilet(): Boolean {
        return oppgave.retries() <= antallFeil + 1
    }

    fun cron(): CronExpression? {
        return oppgave.cron()
    }

    fun erScheduledOppgave(): Boolean {
        return cron() != null
    }
}