package no.nav.aap.behandlingsflyt.flyt.flate

import no.nav.aap.motor.OppgaveStatus

class OppgaveDto(
    val oppgaveType: String,
    val status: OppgaveStatus
)