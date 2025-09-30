package no.nav.aap.behandlingsflyt.hendelse.kafka.klage

import no.nav.aap.behandlingsflyt.hendelse.mottak.MottattHendelseService
import no.nav.aap.behandlingsflyt.hendelse.kafka.KafkaConsumerConfig
import no.nav.aap.behandlingsflyt.hendelse.kafka.KafkaKonsument
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.KabalHendelseKafkaMelding
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.KabalHendelseKilde
import no.nav.aap.behandlingsflyt.prosessering.KafkaFeilJobbUtfører
import no.nav.aap.behandlingsflyt.prosessering.Meldingkilde
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.motor.FlytJobbRepository
import no.nav.person.pdl.leesah.Personhendelse
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.UUID
import javax.sql.DataSource

const val KABAL_EVENT_TOPIC = "klage.behandling-events.v1"

class KabalKafkaKonsument(
    config: KafkaConsumerConfig,
    pollTimeout: Duration = Duration.ofSeconds(10L),
    private val dataSource: DataSource,
    private val repositoryRegistry: RepositoryRegistry
) : KafkaKonsument<String, String>(
    topic = KABAL_EVENT_TOPIC,
    config = config,
    pollTimeout = pollTimeout,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun håndter(meldinger: ConsumerRecords<String, String>) {
        meldinger.forEach(::håndter)
    }

    fun håndter(melding: ConsumerRecord<String, String>) {
        log.info(
            "Behandler klage-record med id: {}, partition {}, offset: {}",
            melding.key(),
            melding.partition(),
            melding.offset(),
        )
        håndter(melding.value())
    }

    fun håndter(meldingVerdi: String) {
        val hendelsekilde = DefaultJsonMapper.fromJson<KabalHendelseKilde>(meldingVerdi)
        if (hendelsekilde.kilde == Fagsystem.KELVIN.name) {
            log.info(
                "Håndterer klagehendelse ${hendelsekilde.eventId}",
            )
            dataSource.transaction { connection ->
                val repositoryProvider = repositoryRegistry.provider(connection)
                val flytjobbRepository = FlytJobbRepository(connection)

                val behandlingRepository: BehandlingRepository = repositoryProvider.provide()
                val saksnummer =
                    try {
                        behandlingRepository.finnSaksnummer(BehandlingReferanse(UUID.fromString(hendelsekilde.kildeReferanse)))
                    } catch (e: Exception) {
                        log.info(
                            "Kunne ikke finne saksnummer for klagehendelse med id ${hendelsekilde.eventId}, oppretter feiljobb",
                            e
                        )
                        flytjobbRepository.leggTil(
                            KafkaFeilJobbUtfører.nyJobb(Meldingkilde.KABAL, meldingVerdi)
                        )
                        return@transaction
                    }

                val klageHendelse = DefaultJsonMapper.fromJson<KabalHendelseKafkaMelding>(meldingVerdi)

                val hendelseService = MottattHendelseService(repositoryProvider)
                hendelseService.registrerMottattHendelse(klageHendelse.tilInnsending(saksnummer))
            }
        }
    }

}

enum class Fagsystem {
    KELVIN,
    AO01 // Arena
}
