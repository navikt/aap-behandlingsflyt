package no.nav.aap.behandlingsflyt

import io.confluent.kafka.serializers.KafkaAvroDeserializer
import io.confluent.kafka.serializers.KafkaAvroSerializer
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.ArbeidsGradering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.Underveisperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisÅrsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.hendelse.kafka.KafkaConsumerConfig
import no.nav.aap.behandlingsflyt.hendelse.kafka.SchemaRegistryConfig
import no.nav.aap.behandlingsflyt.hendelse.kafka.person.PdlHendelseKafkaKonsument
import no.nav.aap.behandlingsflyt.repository.postgresRepositoryRegistry
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.person.pdl.leesah.Endringstype
import no.nav.person.pdl.leesah.Personhendelse
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.StringDeserializer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.kafka.KafkaContainer
import org.apache.kafka.common.serialization.StringSerializer
import org.testcontainers.utility.DockerImageName
import java.time.Duration
import java.time.Instant
import java.util.Properties
import javax.sql.DataSource
import kotlin.concurrent.thread
import kotlin.test.Test

class
PdlHendelseKafkaKonsumentTest {

    companion object {
        val kafka: KafkaContainer = KafkaContainer(DockerImageName.parse("apache/kafka-native:4.1.0"))
            .withReuse(true)
            .waitingFor(Wait.forListeningPort())
            .withStartupTimeout(Duration.ofSeconds(60))

        lateinit var dataSource: TestDataSource
        val repositoryRegistry = postgresRepositoryRegistry

        @BeforeAll
        @JvmStatic
        internal fun beforeAll() {
            dataSource = TestDataSource()
            kafka.start()
        }

        @AfterAll
        @JvmStatic
        internal fun afterAll() {
            kafka.stop()
            dataSource.close()
        }
    }

    val konsument = PdlHendelseKafkaKonsument(
        testConfig(kafka.bootstrapServers),
        dataSource = dataSource,
        repositoryRegistry = repositoryRegistry,
        pollTimeout = Duration.ofMillis(50),
    )

    @Test
    fun `PdlHendelseKafkaKonsument konsumerer Avro Personhendelse`() {

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
            put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.bootstrapServers)
            put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java.name)
            put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer::class.java.name)
            put("schema.registry.url", "mock://schema-registry")
        }

        KafkaProducer<String, Personhendelse>(producerProps).use { producer ->
            producer.send(ProducerRecord(topic, avroHendelse.hendelseId, avroHendelse))
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


private fun testConfig(brokers: String) = KafkaConsumerConfig<String, Personhendelse>(
    applicationId = "behandlingsflyt-test",
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
    })

