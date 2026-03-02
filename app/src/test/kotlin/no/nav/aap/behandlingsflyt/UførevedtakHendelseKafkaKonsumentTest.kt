package no.nav.aap.behandlingsflyt

import no.nav.aap.behandlingsflyt.help.FakePdlGateway
import no.nav.aap.behandlingsflyt.hendelse.kafka.KafkaConsumerConfig
import no.nav.aap.behandlingsflyt.hendelse.kafka.SchemaRegistryConfig
import no.nav.aap.behandlingsflyt.hendelse.kafka.uføre.UførevedtakKafkaKonsument
import no.nav.aap.behandlingsflyt.integrasjon.createGatewayProvider
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.UførevedtakKafkaMelding
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.UførevedtakResultat
import no.nav.aap.behandlingsflyt.repository.postgresRepositoryRegistry
import no.nav.aap.behandlingsflyt.test.AlleAvskruddUnleash
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.komponenter.json.DefaultJsonMapper
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.slf4j.LoggerFactory
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.kafka.KafkaContainer
import org.testcontainers.utility.DockerImageName
import java.time.Duration
import java.time.LocalDate
import java.util.*
import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.time.Duration.Companion.milliseconds

class UførevedtakHendelseKafkaKonsumentTest {

    companion object {
        private const val UFØRE_KAFKA_TOPIC = "lokal.kafkatopic.ufore-vedtak"
        private val logger = LoggerFactory.getLogger(UførevedtakKafkaKonsument::class.java)
        val kafka: KafkaContainer = KafkaContainer(DockerImageName.parse("apache/kafka-native:4.1.0"))
            .withReuse(true)
            .waitingFor(Wait.forListeningPort())
            .withStartupTimeout(Duration.ofSeconds(60))
            .withLogConsumer { Slf4jLogConsumer(logger) }

        private lateinit var dataSource: TestDataSource
        val repositoryRegistry = postgresRepositoryRegistry

        @BeforeAll
        @JvmStatic
        internal fun beforeAll() {
            dataSource = TestDataSource()
            kafka.start()
            System.setProperty("INTEGRASJON_UFORE_VEDTAK_TOPIC", UFØRE_KAFKA_TOPIC)
        }

        @AfterAll
        @JvmStatic
        internal fun afterAll() {
            kafka.stop()
            dataSource.close()
        }
    }

    val konsument = UførevedtakKafkaKonsument(
        config = testConfig(kafka.bootstrapServers),
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

        val uførevedtakMelding = UførevedtakKafkaMelding(
            personid = "12345678901",
            virkningsdato = LocalDate.now(),
            resultat = UførevedtakResultat.AVSL,
            avslag12_5 = false
        )
        val producerProps = Properties().apply {
            put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.bootstrapServers)
            put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java.name)
            put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java.name)
        }

        KafkaProducer<String, String>(producerProps).use { producer ->
            val value = DefaultJsonMapper.toJson(uførevedtakMelding)
            producer.send(
                ProducerRecord(
                    UFØRE_KAFKA_TOPIC,
                    uførevedtakMelding.personid,
                    value
                )
            )
            producer.flush()
        }


        val pollThread = thread(start = true) {
            konsument.konsumer()
        }

        while (konsument.antallMeldinger == 0) {
            Thread.sleep(100)
        }

        assertThat(konsument.antallMeldinger).isEqualTo(1)

        konsument.lukk()
        kafka.stop()
        pollThread.join()
    }
}


private fun testConfig(brokers: String) = KafkaConsumerConfig<String, String>(
    applicationId = "behandlingsflyt-test",
    brokers = brokers,
    ssl = null,
    schemaRegistry = SchemaRegistryConfig(
        url = "mock://schema-registry",
        user = "",
        password = "",
    )
)

