package no.nav.aap.behandlingsflyt.kafka

import no.nav.aap.behandlingsflyt.hendelse.avløp.BehandlingHendelseServiceFactory
import no.nav.aap.behandlingsflyt.integrasjon.createGatewayProvider
import no.nav.aap.behandlingsflyt.prosessering.HendelseMottattHåndteringJobbUtfører
import no.nav.aap.behandlingsflyt.prosessering.KafkaFeilJobbUtfører
import no.nav.aap.behandlingsflyt.repository.postgresRepositoryRegistry
import no.nav.aap.behandlingsflyt.test.AlleAvskruddUnleash
import no.nav.aap.behandlingsflyt.test.FakeAnsattInfoGateway
import no.nav.aap.behandlingsflyt.test.FakeEnhetGateway
import no.nav.aap.behandlingsflyt.test.FakeOppgavestyringGateway
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.motor.testutil.ManuellMotorImpl
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.slf4j.LoggerFactory
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.kafka.KafkaContainer
import org.testcontainers.utility.DockerImageName
import java.time.Duration

open class AbstractKafkaKonsumentTest {
    companion object {
        private val logger = LoggerFactory.getLogger(AbstractKafkaKonsumentTest::class.java)
        const val UFØRE_KAFKA_TOPIC = "lokal.kafkatopic.ufore-vedtak"
        val kafka: KafkaContainer = KafkaContainer(DockerImageName.parse("apache/kafka-native:4.1.0"))
            .withReuse(true)
            .waitingFor(Wait.forListeningPort())
            .withStartupTimeout(Duration.ofSeconds(60))
            .withLogConsumer { Slf4jLogConsumer(logger) }

        lateinit var dataSource: TestDataSource
        lateinit var motor: ManuellMotorImpl
        val repositoryRegistry = postgresRepositoryRegistry

        @BeforeAll
        @JvmStatic
        internal fun beforeAll() {
            System.setProperty("INTEGRASJON_UFORE_VEDTAK_TOPIC",
                UFØRE_KAFKA_TOPIC
            )
            dataSource = TestDataSource()
            motor = ManuellMotorImpl(
                dataSource,
                jobber = listOf(HendelseMottattHåndteringJobbUtfører, KafkaFeilJobbUtfører),
                repositoryRegistry = repositoryRegistry,
                gatewayProvider = createGatewayProvider {
                    register<AlleAvskruddUnleash>()
                    register<FakeAnsattInfoGateway>()
                    register<FakeEnhetGateway>()
                    register<FakeOppgavestyringGateway>()
                    register<BehandlingHendelseServiceFactory>()
                }
            )
            motor.start()
            kafka.start()
        }

        @AfterAll
        @JvmStatic
        internal fun afterAll() {
            kafka.stop()
            motor.stop()
            dataSource.close()
        }
    }


}
