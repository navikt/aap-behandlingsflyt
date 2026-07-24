package no.nav.aap.behandlingsflyt

import no.nav.aap.behandlingsflyt.help.FakePdlGateway
import no.nav.aap.behandlingsflyt.hendelse.kafka.KafkaConsumerConfig
import no.nav.aap.behandlingsflyt.hendelse.kafka.SchemaRegistryConfig
import no.nav.aap.behandlingsflyt.hendelse.kafka.uføre.UførevedtakKafkaKonsument
import no.nav.aap.behandlingsflyt.integrasjon.createGatewayProvider
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.UførevedtakKafkaMelding
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.UførevedtakResultat
import no.nav.aap.behandlingsflyt.test.AlleAvskruddUnleash
import no.nav.aap.behandlingsflyt.test.MockDataSource
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.inMemoryRepositoryRegistry
import no.nav.aap.komponenter.json.DefaultJsonMapper
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.util.RestoreSystemProperties
import java.time.LocalDate
import java.util.Properties
import java.util.UUID
import kotlin.test.Test
import kotlin.time.Duration.Companion.milliseconds

@RestoreSystemProperties
class UførevedtakHendelseKafkaKonsumentTest {

    companion object {
        private const val UFØRE_KAFKA_TOPIC = "lokal.kafkatopic.ufore-vedtak"

        private lateinit var dataSource: MockDataSource
        val repositoryRegistry = inMemoryRepositoryRegistry

        @BeforeAll
        @JvmStatic
        internal fun beforeAll() {
            dataSource = MockDataSource()
            System.setProperty("INTEGRASJON_UFORE_VEDTAK_TOPIC", UFØRE_KAFKA_TOPIC)
        }
    }

    val konsument = UførevedtakKafkaKonsument(
        config = testConfig(SharedKafkaTestContainer.kafka.bootstrapServers),
        dataSource = dataSource,
        repositoryRegistry = repositoryRegistry,
        gatewayProvider = createGatewayProvider {
            register<AlleAvskruddUnleash>()
            register<FakePdlGateway>()
        },
        pollTimeout = 50.milliseconds,
    )

    @Test
    fun `Uførevedtakhendelse konsumeres av kafka`() {
        val pollThread = startConsumerThread {
            konsument.konsumer()
        }

        try {
            val uførevedtakMelding = UførevedtakKafkaMelding(
                personId = "12345678901",
                virkningsdato = LocalDate.now(),
                resultat = UførevedtakResultat.AVSL,
                avslag12_5 = false
            )
            val producerProps = Properties().apply {
                put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, SharedKafkaTestContainer.kafka.bootstrapServers)
                put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java.name)
                put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java.name)
            }

            KafkaProducer<String, String>(producerProps).use { producer ->
                val value = DefaultJsonMapper.toJson(uførevedtakMelding)
                producer.send(
                    ProducerRecord(
                        UFØRE_KAFKA_TOPIC,
                        uførevedtakMelding.personId,
                        value
                    )
                )
                producer.flush()
            }

            awaitAtMost("Konsumenten mottok ikke forventet uføre-melding") {
                konsument.antallMeldinger == 1 || !pollThread.isAlive
            }

            assertThat(konsument.antallMeldinger)
                .withFailMessage("Konsumenten ble lukket uten å motta forventet melding")
                .isEqualTo(1)
        } finally {
            stopConsumerThread(konsument::lukk, pollThread)
        }

    }
}


private fun testConfig(brokers: String) = KafkaConsumerConfig(
    applicationId = "behandlingsflyt-test-${UUID.randomUUID()}",
    brokers = brokers,
    ssl = null,
    schemaRegistry = SchemaRegistryConfig(
        url = "mock://schema-registry",
        user = "",
        password = "",
    )
)
