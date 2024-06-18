package no.nav.aap.behandlingsflyt.flyt.flate

import no.nav.aap.motor.JobbStatus

class JobbDto(
    val oppgaveType: String,
    val status: JobbStatus,
    val antallFeilendeForsøk: Int = 0,
    val feilmelding: String? = null
)