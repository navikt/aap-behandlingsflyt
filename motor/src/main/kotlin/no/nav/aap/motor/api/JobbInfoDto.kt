package no.nav.aap.motor.api

import no.nav.aap.motor.JobbStatus

class JobbInfoDto(
    val id: Long,
    val type: String,
    val status: JobbStatus,
    val metadata: Map<String, String>,
    val antallFeilendeForsøk: Int = 0,
    val feilmelding: String? = null
)