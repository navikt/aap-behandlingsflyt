package no.nav.aap.behandlingsflyt

import org.assertj.core.api.Assertions.assertThat
import org.slf4j.LoggerFactory
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.kafka.KafkaContainer
import org.testcontainers.utility.DockerImageName
import java.time.Duration
import kotlin.concurrent.thread
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

internal object SharedKafkaTestContainer {
    private val logger = LoggerFactory.getLogger(SharedKafkaTestContainer::class.java)

    val kafka: KafkaContainer by lazy {
        KafkaContainer(DockerImageName.parse("apache/kafka-native:4.1.0"))
            .waitingFor(Wait.forListeningPort())
            .withStartupTimeout(Duration.ofSeconds(180))
            .withLogConsumer { Slf4jLogConsumer(logger) }
            .also { it.start() }
    }
}

internal fun awaitAtMost(
    description: String,
    timeout: kotlin.time.Duration = 20.seconds,
    interval: kotlin.time.Duration = 100.milliseconds,
    condition: () -> Boolean,
) {
    val deadlineNanos = System.nanoTime() + timeout.inWholeNanoseconds
    while (!condition() && System.nanoTime() < deadlineNanos) {
        Thread.sleep(interval.inWholeMilliseconds)
    }
    assertThat(condition())
        .withFailMessage("$description (timeout: $timeout)")
        .isTrue()
}

internal fun startConsumerThread(block: () -> Unit): Thread =
    thread(start = true, isDaemon = true) { block() }

internal fun stopConsumerThread(
    stopConsumer: () -> Unit,
    consumerThread: Thread,
    joinTimeout: kotlin.time.Duration = 5.seconds,
) {
    stopConsumer()
    consumerThread.join(joinTimeout.inWholeMilliseconds)
    assertThat(consumerThread.isAlive)
        .withFailMessage("Konsument-tråd stoppet ikke innen $joinTimeout")
        .isFalse()
}
