package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.motor.ProvidersJobbSpesifikasjon
import no.nav.aap.motor.cron.CronExpression

class TriggBarnetilleggSatsJobbUtfører : JobbUtfører {
    override fun utfør(input: JobbInput) {

    }


    companion object : ProvidersJobbSpesifikasjon {
        override val type = "satsendring"
        override val navn = "Finn og prosesser saker med barnetillegg."
        override val beskrivelse = ""

        /**
         * Kjør hver 2 januar kl 09:00.
         */
        override val cron: CronExpression = CronExpression.create("0 9 2 1 *")

        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): TriggBarnetilleggSatsJobbUtfører {
            return TriggBarnetilleggSatsJobbUtfører(

            )
        }
    }
}