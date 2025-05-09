package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.RepositoryRegistry
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.Jobb
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.motor.cron.CronExpression

class OpprettJobbForFastsattPeriodeJobbUtfører(
    private val flytJobbRepository: FlytJobbRepository,
    private val sakRepository: SakRepository,
) : JobbUtfører {

    override fun utfør(input: JobbInput) {
        /* TODO: optimaliser */
        for (sak in sakRepository.finnAlle()) {
            flytJobbRepository.leggTil(JobbInput(OpprettBehandlingFastsattPeriodePassertJobbUtfører).forSak(sak.id.toLong()))
        }
    }


    companion object : Jobb {
        override fun konstruer(connection: DBConnection): JobbUtfører {
            val provider = RepositoryRegistry.provider(connection)
            return OpprettJobbForFastsattPeriodeJobbUtfører(
                flytJobbRepository = provider.provide(),
                sakRepository = provider.provide(),
            )
        }

        override fun type() = "batch.OpprettJobbForFastsattPeriode"

        override fun navn() = "Start jobb for å sjekke behov for revurdering pga manglende meldekort"

        override fun beskrivelse() = """
            Start jobb for å sjekke om fastsatt dager er passert.
            """.trimIndent()

        override fun cron() = CronExpression.createWithoutSeconds("10 2 * * 2")
    }
}
