package no.nav.aap.behandlingsflyt.hendelse.kafka.klage

import no.nav.aap.behandlingsflyt.hendelse.MottattHendelseService
import no.nav.aap.behandlingsflyt.hendelse.kafka.KafkaConsumerConfig
import no.nav.aap.behandlingsflyt.hendelse.kafka.KafkaKonsument
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.KabalHendelseId
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Innsending
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.KabalHendelseKafkaMelding
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.verdityper.dokument.Kanal
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.LocalDateTime
import java.util.UUID
import javax.sql.DataSource

const val KABAL_EVENT_TOPIC = "klage.behandling-events.v1"

class KabalKafkaKonsument(
    config: KafkaConsumerConfig,
    pollTimeout: Duration = Duration.ofSeconds(10L),
    private val dataSource: DataSource,
    private val repositoryRegistry: RepositoryRegistry
): KafkaKonsument(
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
        val klageHendelse = DefaultJsonMapper.fromJson<KabalHendelseKafkaMelding>(meldingVerdi)
        if (klageHendelse.kilde == Fagsystem.KELVIN.name) {
            log.info(
                "Håndterer klagehendelse ${klageHendelse.eventId}",
            )
            dataSource.transaction {
                val repositoryProvider = repositoryRegistry.provider(it)
                val behandlingRepository: BehandlingRepository = repositoryProvider.provide()
                val saksnummer =
                    behandlingRepository.finnSaksnummer(BehandlingReferanse(UUID.fromString(klageHendelse.kildeReferanse)))
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

private fun KabalHendelseKafkaMelding.tilInnsending(saksnummer: Saksnummer) =
    Innsending(
        saksnummer = saksnummer,
        referanse = InnsendingReferanse(KabalHendelseId(value = this.eventId)),
        type = InnsendingType.KABAL_HENDELSE,
        kanal = Kanal.DIGITAL,
        mottattTidspunkt = LocalDateTime.now(),
        melding = this.tilKabalHendelseV0()
    )