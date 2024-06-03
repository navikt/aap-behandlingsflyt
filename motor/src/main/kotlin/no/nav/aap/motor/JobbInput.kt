package no.nav.aap.motor

import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import no.nav.aap.verdityper.sakogbehandling.SakId
import java.time.LocalDateTime

class JobbInput(val jobb: Jobb) {

    internal var id: Long? = null
    private var sakId: SakId? = null
    private var behandlingId: BehandlingId? = null
    private var nesteKjøring: LocalDateTime? = null
    private var antallFeil: Long = 0
    private var status: JobbStatus = JobbStatus.KLAR

    internal fun medId(id: Long): JobbInput {
        this.id = id
        return this
    }

    internal fun medStatus(status: JobbStatus): JobbInput {
        this.status = status
        return this
    }

    fun forBehandling(sakId: SakId?, behandlingId: BehandlingId?): JobbInput {
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

    fun status(): JobbStatus {
        return status
    }

    fun behandlingId(): BehandlingId {
        return behandlingId!!
    }

    fun behandlingIdOrNull(): BehandlingId? {
        return behandlingId
    }

    fun medAntallFeil(antallFeil: Long): JobbInput {
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
        return jobb.type()
    }

    override fun toString(): String {
        return "[${jobb.type()}] - id = $id, sakId = $sakId, behandlingId = $behandlingId"
    }

    fun medNesteKjøring(nesteKjøring: LocalDateTime): JobbInput {
        this.nesteKjøring = nesteKjøring
        return this
    }

    fun skalMarkeresSomFeilet(): Boolean {
        return jobb.retries() <= antallFeil + 1
    }

    fun cron(): CronExpression? {
        return jobb.cron()
    }

    fun erScheduledOppgave(): Boolean {
        return cron() != null
    }
}