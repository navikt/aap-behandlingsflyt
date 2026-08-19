package no.nav.aap.behandlingsflyt

import no.nav.aap.behandlingsflyt.help.FakePdlGateway
import no.nav.aap.behandlingsflyt.hendelse.kafka.KafkaConsumerConfig
import no.nav.aap.behandlingsflyt.hendelse.kafka.SchemaRegistryConfig
import no.nav.aap.behandlingsflyt.hendelse.kafka.foreldrepenger.FORELDREPENGEVEDTAK_EVENT_TOPIC
import no.nav.aap.behandlingsflyt.hendelse.kafka.foreldrepenger.ForeldrepengevedtakKafkaKonsument
import no.nav.aap.behandlingsflyt.integrasjon.createGatewayProvider
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.ForeldrepengevedtakKafkaMelding
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
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.*
import kotlin.test.Test
import kotlin.time.Duration.Companion.milliseconds

class ForeldrepengevedtakHendelseKafkaKonsumentTest {

    companion object {
        private lateinit var dataSource: MockDataSource
        val repositoryRegistry = inMemoryRepositoryRegistry

        @BeforeAll
        @JvmStatic
        internal fun beforeAll() {
            dataSource = MockDataSource()
        }
    }

    val konsument = ForeldrepengevedtakKafkaKonsument(
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
    fun `Foreldrepengevedtakhendelse konsumeres av kafka`() {
        val pollThread = startConsumerThread {
            konsument.konsumer()
        }

        try {
            val foreldrepengevedtakhendelse = ForeldrepengevedtakKafkaMelding(
                personidentifikator = "12345678901",
                tidspunkt = OffsetDateTime.now(ZoneOffset.UTC),
                tema = "FOR",
            )
            val producerProps = Properties().apply {
                put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, SharedKafkaTestContainer.kafka.bootstrapServers)
                put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java.name)
                put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java.name)
            }

            KafkaProducer<String, String>(producerProps).use { producer ->
                val value = DefaultJsonMapper.toJson(foreldrepengevedtakhendelse)
                producer.send(
                    ProducerRecord(
                        FORELDREPENGEVEDTAK_EVENT_TOPIC,
                        foreldrepengevedtakhendelse.personidentifikator,
                        value
                    )
                )
                producer.flush()
            }

            awaitAtMost("Konsumenten mottok ikke forventet foreldrepenge-melding") {
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
    applicationId = "behandlingsflyt-test-${java.util.UUID.randomUUID()}",
    brokers = brokers,
    ssl = null,
    schemaRegistry = SchemaRegistryConfig(
        url = "mock://schema-registry",
        user = "",
        password = "",
    )
)
