package no.nav.aap.behandlingsflyt.flyt.flate

import no.nav.aap.motor.JobbStatus

class OppgaveDto(
    val oppgaveType: String,
    val status: JobbStatus
)