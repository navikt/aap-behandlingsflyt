package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import io.ktor.util.collections.ConcurrentSet
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput

object InMemoryFlytJobbRepository: FlytJobbRepository {
    private val jobber = ConcurrentSet<JobbInput>()

    override fun leggTil(jobbInput: JobbInput) {
        jobber.add(jobbInput)
    }

    override fun hentJobberForBehandling(id: Long) =
        jobber.filter { it.behandlingIdOrNull() == id }

    override fun hentFeilmeldingForOppgave(id: Long) = ""
}