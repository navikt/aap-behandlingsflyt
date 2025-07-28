package no.nav.aap.behandlingsflyt

import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepositoryImpl
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.hendelse.kafka.klage.KabalKafkaKonsument
import no.nav.aap.behandlingsflyt.hendelse.kafka.KafkaConsumerConfig
import org.junit.jupiter.api.Test
import no.nav.aap.behandlingsflyt.hendelse.kafka.SchemaRegistryConfig
import no.nav.aap.behandlingsflyt.hendelse.kafka.klage.KABAL_EVENT_TOPIC
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.KabalHendelseId
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.BehandlingDetaljer
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.BehandlingEventType
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.KabalHendelseKafkaMelding
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.KlageUtfall
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.KlagebehandlingAvsluttetDetaljer
import no.nav.aap.behandlingsflyt.prosessering.HendelseMottattHåndteringJobbUtfører
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.postgresRepositoryRegistry
import no.nav.aap.behandlingsflyt.repository.sak.PersonRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.ÅrsakTilBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.test.FakeUnleash
import no.nav.aap.behandlingsflyt.test.ident
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.komponenter.gateway.GatewayRegistry
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.motor.Motor
import no.nav.aap.motor.testutil.TestUtil
import no.nav.aap.verdityper.dokument.Kanal
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import java.time.LocalDateTime
import java.util.UUID
import org.testcontainers.kafka.KafkaContainer
import org.testcontainers.utility.DockerImageName
import java.time.Duration
import java.time.LocalDate
import java.util.Properties
import kotlin.concurrent.thread

class KabalKafkaKonsumentTest {
    companion object {
        private val dataSource = InitTestDatabase.freshDatabase()
        private val repositoryRegistry = postgresRepositoryRegistry
        private val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))
        private val motor =
            Motor(
                dataSource,
                1,
                jobber = listOf(HendelseMottattHåndteringJobbUtfører),
                repositoryRegistry = repositoryRegistry
            )
        val kafka = KafkaContainer(DockerImageName.parse("apache/kafka-native:4.0.0"))
            .withReuse(true).withStartupTimeout(Duration.ofSeconds(60))
        private val util =
            TestUtil(dataSource, listOf(HendelseMottattHåndteringJobbUtfører.type))

        @BeforeAll
        @JvmStatic
        internal fun beforeAll() {
            GatewayRegistry.register<FakeUnleash>()
            motor.start()
            kafka.start()

        }

        @AfterAll
        @JvmStatic
        internal fun afterAll() {
            motor.stop()
            kafka.stop()
        }
    }

    @Test
    fun `Kan motta og lagre ned hendelse fra Kabal`() {
        val klagebehandling = dataSource.transaction { connection ->
            val sak = sak(connection)
            finnEllerOpprettBehandling(connection, sak)
            finnEllerOpprettBehandling(connection, sak, ÅrsakTilBehandling.MOTATT_KLAGE)
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

        util.ventPåSvar(klagebehandling.sakId.id, klagebehandling.id.id)
        val svarFraAnderinstansBehandling = dataSource.transaction { connection ->
            val behandlinger = BehandlingRepositoryImpl(connection).hentAlleFor(
                klagebehandling.sakId,
                listOf(TypeBehandling.SvarFraAndreinstans)
            )
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
        assertThat(hendelser.first().status).isEqualTo(no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Status.BEHANDLET)
        assertThat(hendelser.first().strukturertDokument).isNotNull
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

    private fun testConfig(brokers: String) = KafkaConsumerConfig(
        applicationId = "behandlingsflyt-test",
        brokers = brokers,
        ssl = null,
        schemaRegistry = SchemaRegistryConfig(
            url = "mock://kafka",
            user = "",
            password = "",
        )
    )

    private fun sak(connection: DBConnection): Sak {
        return PersonOgSakService(
            FakePdlGateway,
            PersonRepositoryImpl(connection),
            SakRepositoryImpl(connection)
        ).finnEllerOpprett(ident(), periode)
    }

}