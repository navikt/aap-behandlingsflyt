package no.nav.aap.behandlingsflyt.flyt.flate.visning

import no.nav.aap.motor.api.JobbInfoDto

data class Prosessering(val status: ProsesseringStatus, val ventendeOppgaver: List<JobbInfoDto>)
