package no.nav.aap.behandlingsflyt.hendelse.kafka

import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.MockConsumer
import org.apache.kafka.clients.consumer.OffsetResetStrategy
import org.apache.kafka.common.TopicPartition
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class KafkaKonsumentTest {
    @Test
    fun `committer offset først etter at meldingshåndtereren har behandlet meldingene`() {
        val partisjon = TopicPartition("hendelser", 0)
        val kafka = MockConsumer<String, String>(OffsetResetStrategy.EARLIEST)
        val mottatte = mutableListOf<String>()
        val konsument = KafkaKonsument(
            topic = partisjon.topic(),
            konsument = kafka,
            håndter = object : KafkaMeldingsHåndterer<String, String> {
                override val navn = "Testhåndterer"

                override fun håndter(melding: ConsumerRecord<String, String>) {
                    assertThat(kafka.committed(setOf(partisjon))[partisjon]).isNull()
                    mottatte.add(melding.value())
                }
            }
        )
        kafka.schedulePollTask {
            kafka.rebalance(listOf(partisjon))
            kafka.updateBeginningOffsets(mapOf(partisjon to 0L))
            kafka.addRecord(ConsumerRecord(partisjon.topic(), partisjon.partition(), 0L, "nøkkel", "hendelse"))
            kafka.addRecord(ConsumerRecord(partisjon.topic(), partisjon.partition(), 1L, "nøkkel", "neste hendelse"))
        }
        kafka.schedulePollTask {
            assertThat(kafka.committed(setOf(partisjon))[partisjon]?.offset()).isEqualTo(2L)
            konsument.lukk()
        }

        konsument.konsumer()

        assertThat(mottatte).containsExactly("hendelse", "neste hendelse")
        assertThat(konsument.antallMeldinger).isEqualTo(2)
        assertThat(konsument.erLukket()).isTrue()
        assertThat(kafka.closed()).isTrue()
    }

    @Test
    fun `feil i meldingshåndtereren lukker konsumenten uten commit eller ny polling`() {
        val partisjon = TopicPartition("hendelser", 0)
        var committetOffset: Long? = null
        val kafka = object : MockConsumer<String, String>(OffsetResetStrategy.EARLIEST) {
            override fun close(timeout: java.time.Duration) {
                committetOffset = committed(setOf(partisjon))[partisjon]?.offset()
                super.close(timeout)
            }
        }
        var polletNesteMelding = false
        val mottatte = mutableListOf<String>()
        val konsument = KafkaKonsument(
            topic = partisjon.topic(),
            konsument = kafka,
            håndter = object : KafkaMeldingsHåndterer<String, String> {
                override val navn = "Testhåndterer"

                override fun håndter(melding: ConsumerRecord<String, String>) {
                    mottatte.add(melding.value())
                    if (melding.offset() == 1L) {
                        throw IllegalStateException("Kunne ikke behandle hendelsen")
                    }
                }
            }
        )
        kafka.schedulePollTask {
            kafka.rebalance(listOf(partisjon))
            kafka.updateBeginningOffsets(mapOf(partisjon to 0L))
            kafka.addRecord(ConsumerRecord(partisjon.topic(), partisjon.partition(), 0L, "nøkkel", "hendelse"))
            kafka.addRecord(ConsumerRecord(partisjon.topic(), partisjon.partition(), 1L, "nøkkel", "ugyldig hendelse"))
            kafka.addRecord(ConsumerRecord(partisjon.topic(), partisjon.partition(), 2L, "nøkkel", "siste hendelse"))
        }
        kafka.schedulePollTask {
            polletNesteMelding = true
            konsument.lukk()
        }

        konsument.konsumer()

        assertThat(mottatte).containsExactly("hendelse", "ugyldig hendelse")
        assertThat(committetOffset).isNull()
        assertThat(polletNesteMelding).isFalse()
        assertThat(konsument.antallMeldinger).isZero()
        assertThat(konsument.erLukket()).isTrue()
        assertThat(kafka.closed()).isTrue()
    }
}
