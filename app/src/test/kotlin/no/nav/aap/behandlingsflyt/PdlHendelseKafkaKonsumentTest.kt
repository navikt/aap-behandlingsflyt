package no.nav.aap.behandlingsflyt

import io.confluent.kafka.serializers.KafkaAvroDeserializer
import io.confluent.kafka.serializers.KafkaAvroSerializer
import no.nav.aap.behandlingsflyt.help.FakePdlGateway
import no.nav.aap.behandlingsflyt.hendelse.kafka.KafkaConsumerConfig
import no.nav.aap.behandlingsflyt.hendelse.kafka.SchemaRegistryConfig
import no.nav.aap.behandlingsflyt.hendelse.kafka.person.PdlHendelseKafkaKonsument
import no.nav.aap.behandlingsflyt.integrasjon.createGatewayProvider
import no.nav.aap.behandlingsflyt.test.FakeOppgavestyringGateway
import no.nav.aap.behandlingsflyt.test.AlleAvskruddUnleash
import no.nav.aap.behandlingsflyt.test.MockDataSource
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.inMemoryRepositoryRegistry
import no.nav.person.pdl.leesah.Endringstype
import no.nav.person.pdl.leesah.Personhendelse
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import java.time.Instant
import java.util.*
import kotlin.test.Test
import kotlin.time.Duration.Companion.milliseconds

class PdlHendelseKafkaKonsumentTest {

    companion object {
        private lateinit var dataSource: MockDataSource
        val repositoryRegistry = inMemoryRepositoryRegistry

        @BeforeAll
        @JvmStatic
        internal fun beforeAll() {
            dataSource = MockDataSource()
        }
    }

    val konsument = PdlHendelseKafkaKonsument(
        testConfig(SharedKafkaTestContainer.kafka.bootstrapServers),
        dataSource = dataSource,
        repositoryRegistry = repositoryRegistry,
        gatewayProvider = createGatewayProvider {
            register<AlleAvskruddUnleash>()
            register<FakeOppgavestyringGateway>()
            register<FakePdlGateway>()
        },
        pollTimeout = 50.milliseconds,
    )

    @Test
    fun `PdlHendelseKafkaKonsument konsumerer Avro Personhendelse`() {
        val pollThread = startConsumerThread {
            konsument.konsumer()
        }

        try {
            val topic = "pdl.leesah-v1"

            val avroHendelse = Personhendelse.newBuilder()
                .setHendelseId("test-hendelse-1")
                .setPersonidenter(listOf("12345678901"))
                .setMaster("FREG")
                .setOpprettet(Instant.now())
                .setOpplysningstype("DOEDSFALL_V1")
                .setEndringstype(Endringstype.OPPRETTET)
                .build()

            val producerProps = Properties().apply {
                put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, SharedKafkaTestContainer.kafka.bootstrapServers)
                put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java.name)
                put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer::class.java.name)
                put("schema.registry.url", "mock://schema-registry")
            }

            KafkaProducer<String, Personhendelse>(producerProps).use { producer ->
                producer.send(ProducerRecord(topic, avroHendelse.hendelseId, avroHendelse))
                producer.flush()
            }

            awaitAtMost("Konsumenten mottok ikke forventet PDL-hendelse") {
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
    ),
    keyDeserializer =
        StringDeserializer::class.java as Class<out Deserializer<String>>,
    valueDeserializer = KafkaAvroDeserializer::class.java as Class<out Deserializer<Personhendelse>>,
    additionalProperties = Properties().apply {
        put("specific.avro.reader", true)
    }
)
