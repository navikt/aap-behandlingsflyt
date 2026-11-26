package no.nav.aap.behandlingsflyt.hendelse.kafka.inst2

import no.nav.aap.behandlingsflyt.hendelse.kafka.KafkaMeldingsHåndterer
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.InstitusjonsOppholdHendelseKafkaMelding
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.repository.RepositoryRegistry
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import javax.sql.DataSource

class InstitusjonsOppholdMeldingsHåndterer(
    private val håndterHendelse: (String, InstitusjonsOppholdHendelseKafkaMelding) -> Unit
) : KafkaMeldingsHåndterer<String, InstitusjonsOppholdHendelseKafkaMelding> {
    override val navn = "AapBehandlingsflytInstitusjonsOppholdHendelse"

    constructor(
        dataSource: DataSource,
        repositoryRegistry: RepositoryRegistry,
        gatewayProvider: GatewayProvider
    ) : this({ nøkkel, melding ->
        dataSource.transaction { connection ->
            val repositoryProvider = repositoryRegistry.provider(connection)
            InstitusjonsOppholdService(repositoryProvider, gatewayProvider).håndter(nøkkel, melding)
        }
    })

    private val log = LoggerFactory.getLogger(javaClass)

    override fun håndter(melding: ConsumerRecord<String, InstitusjonsOppholdHendelseKafkaMelding>) {
        val meldingKey = "${melding.partition()}-${melding.offset()}"
        if (melding.value().kilde == null) {
            log.warn(
                "Ignorerer institusjonsoppholdhendelse uten kilde. Topic={}, partisjon={}, offset={}",
                melding.topic(),
                melding.partition(),
                melding.offset()
            )
            return
        }
        håndterHendelse(meldingKey, melding.value())
    }
}
