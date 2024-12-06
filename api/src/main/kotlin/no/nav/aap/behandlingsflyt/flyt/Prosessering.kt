package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.behandlingsflyt.flyt.flate.visning.ProsesseringStatus
import no.nav.aap.motor.api.JobbInfoDto

data class Prosessering(val status: ProsesseringStatus, val ventendeOppgaver: List<JobbInfoDto>)
