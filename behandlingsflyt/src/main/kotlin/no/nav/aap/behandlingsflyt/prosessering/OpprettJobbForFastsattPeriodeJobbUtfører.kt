package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.motor.ProvidersJobbSpesifikasjon
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


    companion object : ProvidersJobbSpesifikasjon {
        override fun konstruer(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider): JobbUtfører {
            return OpprettJobbForFastsattPeriodeJobbUtfører(
                flytJobbRepository = repositoryProvider.provide(),
                sakRepository = repositoryProvider.provide(),
            )
        }

        override val type = "batch.OpprettJobbForFastsattPeriode"

        override val navn = "Start jobb for å sjekke behov for revurdering pga manglende meldekort"

        override val beskrivelse = """
            Start jobb for å sjekke om fastsatt dager er passert.
            """.trimIndent()

        override val cron = CronExpression.createWithoutSeconds("10 2 * * 2")
    }
}
