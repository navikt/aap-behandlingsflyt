package no.nav.aap.behandlingsflyt.kafka

import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.motor.ProvidersJobbSpesifikasjon
import org.slf4j.LoggerFactory

class DummyProsesserBehandlingJobbUtfører : JobbUtfører {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun utfør(input: JobbInput) {
    }

    companion object : ProvidersJobbSpesifikasjon {
        override fun konstruer(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider): JobbUtfører {
            return DummyProsesserBehandlingJobbUtfører()
        }

        override val type = "flyt.prosesserBehandling"
        override val navn = "Prosesser behandling"
        override val beskrivelse = "Ansvarlig for å drive prosessen på en gitt behandling"
    }
}
