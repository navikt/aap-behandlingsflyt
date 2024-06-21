package no.nav.aap.behandlingsflyt.flyt.flate

import no.nav.aap.motor.api.JobbInfoDto

class Prosessering(val status: ProsesseringStatus, val ventendeOppgaver: List<JobbInfoDto>)
