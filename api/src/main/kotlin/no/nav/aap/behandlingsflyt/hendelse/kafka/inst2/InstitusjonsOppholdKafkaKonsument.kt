package no.nav.aap.behandlingsflyt.hendelse.kafka.inst2

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.InstitusjonsoppholdGateway
import no.nav.aap.behandlingsflyt.hendelse.kafka.KafkaConsumerConfig
import no.nav.aap.behandlingsflyt.hendelse.kafka.KafkaKonsument
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.InstitusjonsOppholdHendelseKafkaMelding
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.repository.RepositoryRegistry
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.slf4j.LoggerFactory
import javax.sql.DataSource
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

val INSTITUSJONSOPPHOLD_EVENT_TOPIC: String =
    requiredConfigForKey("INTEGRASJON_INSTITUSJONSOPPHOLD_EVENT_TOPIC")

class InstitusjonsOppholdKafkaKonsument(
    config: KafkaConsumerConfig<String, InstitusjonsOppholdHendelseKafkaMelding>,
    pollTimeout: Duration = 10.seconds,
    closeTimeout: Duration = 30.seconds,
    private val dataSource: DataSource,
    private val repositoryRegistry: RepositoryRegistry,
    private val gatewayProvider: GatewayProvider,
    val institusjonsoppholdKlient: InstitusjonsoppholdGateway,
) : KafkaKonsument<String, InstitusjonsOppholdHendelseKafkaMelding>(
    topic = INSTITUSJONSOPPHOLD_EVENT_TOPIC,
    config = config,
    pollTimeout = pollTimeout,
    closeTimeout = closeTimeout,
    consumerName = "AapBehandlingsflytInstitusjonsOppholdHendelse",
) {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun håndter(meldinger: ConsumerRecords<String, InstitusjonsOppholdHendelseKafkaMelding>) {
        meldinger.forEach(::håndter)
    }

    fun håndter(melding: ConsumerRecord<String, InstitusjonsOppholdHendelseKafkaMelding>) {
        log.info(
            "Behandler institusjonsopphold-record med id: ${melding.key()}, partition ${melding.partition()}, offset: ${melding.offset()}, topic: $topic"
        )
        val meldingKey = "${melding.partition()}-${melding.offset()}"
        dataSource.transaction { connection ->
            val repositoryProvider = repositoryRegistry.provider(connection)
            val institusjonsoppholdService = InstitusjonsOppholdService(repositoryProvider, gatewayProvider)
            institusjonsoppholdService.håndter(meldingKey, melding.value())
        }
    }

}
