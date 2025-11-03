package no.nav.aap.behandlingsflyt

import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepositoryImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Status
import no.nav.aap.behandlingsflyt.help.FakePdlGateway
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.hendelse.kafka.KafkaConsumerConfig
import no.nav.aap.behandlingsflyt.hendelse.kafka.SchemaRegistryConfig
import no.nav.aap.behandlingsflyt.hendelse.kafka.klage.KABAL_EVENT_TOPIC
import no.nav.aap.behandlingsflyt.hendelse.kafka.klage.KabalKafkaKonsument
import no.nav.aap.behandlingsflyt.integrasjon.createGatewayProvider
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.KabalHendelseId
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.BehandlingDetaljer
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.BehandlingEventType
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.KabalHendelseKafkaMelding
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.KlageUtfall
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.KlagebehandlingAvsluttetDetaljer
import no.nav.aap.behandlingsflyt.prosessering.HendelseMottattHåndteringJobbUtfører
import no.nav.aap.behandlingsflyt.prosessering.KafkaFeilJobbUtfører
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.postgresRepositoryRegistry
import no.nav.aap.behandlingsflyt.repository.sak.PersonRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.test.FakeUnleash
import no.nav.aap.behandlingsflyt.test.ident
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.motor.Motor
import no.nav.aap.motor.testutil.ManuellMotorImpl
import no.nav.aap.verdityper.dokument.Kanal
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.kafka.KafkaContainer
import org.testcontainers.utility.DockerImageName
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import kotlin.concurrent.thread

class KabalKafkaKonsumentTest {
    companion object {
        private val repositoryRegistry = postgresRepositoryRegistry
        private val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))

        private lateinit var dataSource: TestDataSource
        private lateinit var motor: ManuellMotorImpl

        val kafka: KafkaContainer = KafkaContainer(DockerImageName.parse("apache/kafka-native:4.1.0"))
            .withReuse(true)
            .waitingFor(Wait.forListeningPort())
            .withStartupTimeout(Duration.ofSeconds(60))

        @BeforeAll
        @JvmStatic
        internal fun beforeAll() {
            // Avoid starting testcontainers and motor during class initialization, as it takes a while, and
            // can lead to exceptions with root cause InitializationError.
            dataSource = TestDataSource()
            motor = ManuellMotorImpl(
                    dataSource,
                    jobber = listOf(HendelseMottattHåndteringJobbUtfører, KafkaFeilJobbUtfører),
                    repositoryRegistry = repositoryRegistry,
                    gatewayProvider = createGatewayProvider { register<FakeUnleash>() }
                )
            motor.start()

            kafka.start()
        }

        @AfterAll
        @JvmStatic
        internal fun afterAll() {
            motor.stop()
            kafka.stop()
            dataSource.close()
        }
    }

    @Test
    fun `Kan motta og lagre ned hendelse fra Kabal`() {
        val sak = dataSource.transaction { sak(it) }
        dataSource.transaction { finnEllerOpprettBehandling(it, sak) }
        val klagebehandling = dataSource.transaction { connection ->
            finnEllerOpprettBehandling(connection, sak, Vurderingsbehov.MOTATT_KLAGE)
        }

        val hendelse = lagBehandlingEvent(kilde = "KELVIN", klagebehandling.referanse.toString())
        produserHendelse(
            listOf(hendelse),
            KABAL_EVENT_TOPIC
        )

        val konsument = KabalKafkaKonsument(
            testConfig(kafka.bootstrapServers),
            dataSource = dataSource,
            repositoryRegistry = repositoryRegistry,
            pollTimeout = Duration.ofMillis(50),
        )

        val thread = thread(start = true) {
            while (true) {
                if (konsument.antallMeldinger > 0) {
                    konsument.lukk()
                    return@thread
                }
                Thread.sleep(500L)
            }
        }
        konsument.konsumer()

        thread.join()
        assertThat(konsument.antallMeldinger).isEqualTo(1)
        konsument.lukk()

        motor.kjørJobber()
        val svarFraAnderinstansBehandling = dataSource.transaction { connection ->
            var behandlinger: List<Behandling> = emptyList()
            var tries = 0
            while (tries < 5) {
                behandlinger = BehandlingRepositoryImpl(connection).hentAlleFor(
                    klagebehandling.sakId,
                    listOf(TypeBehandling.SvarFraAndreinstans)
                )
                if (behandlinger.isNotEmpty()) {
                    break
                }
                tries++
                Thread.sleep(200)
            }
            assertThat(behandlinger).hasSize(1)
            behandlinger.first()
        }

        val hendelser = dataSource.transaction { connection ->
            MottattDokumentRepositoryImpl(connection).hentDokumenterAvType(
                sakId = klagebehandling.sakId,
                InnsendingType.KABAL_HENDELSE
            )
        }
        assertThat(hendelser).hasSize(1)
        assertThat(hendelser.first().referanse.asKabalHendelseId).isEqualTo(KabalHendelseId(hendelse.eventId))
        assertThat(hendelser.first().type).isEqualTo(InnsendingType.KABAL_HENDELSE)
        assertThat(hendelser.first().sakId).isEqualTo(klagebehandling.sakId)
        assertThat(hendelser.first().behandlingId).isEqualTo(svarFraAnderinstansBehandling.id)
        assertThat(hendelser.first().kanal).isEqualTo(Kanal.DIGITAL)
        assertThat(hendelser.first().status).isEqualTo(Status.BEHANDLET)
        assertThat(hendelser.first().strukturertDokument).isNotNull
    }

    @Test
    fun `Skal opprette feiljobb for meldinger som skal til Kelvin, men som vi ikke finner saksnummer for`() {
        val konsument = KabalKafkaKonsument(
            testConfig(kafka.bootstrapServers),
            dataSource = dataSource,
            repositoryRegistry = repositoryRegistry,
            pollTimeout = Duration.ofMillis(50),
        )
        
        val melding = """{"kilde": "KELVIN", "eventId": "123", "kildeReferanse": "123", "resten": "Noe tull"}"""

        konsument.håndter(melding)

        dataSource.transaction { connection ->
            val jobber = hentJobber(connection)
            assertThat(jobber).hasSize(1)
            assertThat(jobber.first().type).isEqualTo(KafkaFeilJobbUtfører.type)
            assertThat(jobber.first().parameters.trimIndent()).isEqualTo("meldingkilde=KABAL")
            assertThat(jobber.first().payload).isEqualTo(melding)
        }
    }

    private fun lagBehandlingEvent(kilde: String, kildereferanse: String): KabalHendelseKafkaMelding {
        return KabalHendelseKafkaMelding(
            UUID.randomUUID(),
            kildeReferanse = kildereferanse,
            kilde = kilde,
            "kabalReferanse",
            BehandlingEventType.KLAGEBEHANDLING_AVSLUTTET,
            BehandlingDetaljer(
                KlagebehandlingAvsluttetDetaljer(
                    LocalDateTime.now().minusDays(1),
                    KlageUtfall.MEDHOLD,
                    listOf("123", "345"),
                ),
            ),
        )
    }

    private fun produserHendelse(hendelser: List<KabalHendelseKafkaMelding>, topic: String) {
        val producerProps = Properties().apply {
            put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.bootstrapServers)
            put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java.name)
            put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java.name)
        }

        KafkaProducer<String, String>(producerProps).use { producer ->
            hendelser.forEach { hendelse ->
                val serialisert = DefaultJsonMapper.toJson(hendelse)
                val record = ProducerRecord(topic, hendelse.eventId.toString(), serialisert)
                producer.send(record)

            }
            producer.flush()
        }
    }

    private fun testConfig(brokers: String) = KafkaConsumerConfig<String, String>(
        applicationId = "behandlingsflyt-test",
        brokers = brokers,
        ssl = null,
        schemaRegistry = SchemaRegistryConfig(
            url = "mock://kafka",
            user = "",
            password = "",
        ),
    )

    private fun sak(connection: DBConnection): Sak {
        return PersonOgSakService(
            FakePdlGateway,
            PersonRepositoryImpl(connection),
            SakRepositoryImpl(connection)
        ).finnEllerOpprett(ident(), periode)
    }

    private data class JobbInfo(
        val type: String,
        val payload: String,
        val parameters: String

    )

    private fun hentJobber(connection: DBConnection): List<JobbInfo> {
        return connection.queryList(
            """
            SELECT * FROM JOBB
        """.trimIndent()
        ) {
            setRowMapper { row ->
                JobbInfo(
                    row.getString("type"),
                    row.getString("payload"),
                    row.getString("parameters")
                )
            }
        }
    }

}