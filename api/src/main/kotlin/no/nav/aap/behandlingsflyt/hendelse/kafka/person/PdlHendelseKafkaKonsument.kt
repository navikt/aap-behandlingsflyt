package no.nav.aap.behandlingsflyt.hendelse.kafka.person

import no.nav.aap.behandlingsflyt.hendelse.MottattHendelseService
import no.nav.aap.behandlingsflyt.hendelse.kafka.KafkaConsumerConfig
import no.nav.aap.behandlingsflyt.hendelse.kafka.KafkaKonsument
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.PdlHendelseId
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Innsending
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.PdlHendelseKafkaMelding
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

const val PDL_HENDELSE_TOPIC = "pdl.leesah-v1"

class PdlHendelseKafkaKonsument(
    config: KafkaConsumerConfig,
    pollTimeout: Duration = Duration.ofSeconds(10L),
    private val dataSource: DataSource,
    private val repositoryRegistry: RepositoryRegistry
) : KafkaKonsument(
    topic = PDL_HENDELSE_TOPIC,
    config = config,
    pollTimeout = pollTimeout,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun håndter(meldinger: ConsumerRecords<String, String>) {
        meldinger.forEach(::håndter)
    }

    fun håndter(melding: ConsumerRecord<String, String>) {
        log.info(
            "Behandler hendelse fra PDL med id: {}, partition {}, offset: {}",
            melding.key(),
            melding.partition(),
            melding.offset(),
        )
        dataSource.transaction {
            val repositoryProvider = repositoryRegistry.provider(it)
            val behandlingRepository: BehandlingRepository = repositoryProvider.provide()
            // Finn riktig sak og behandling for person
            val personHendelse = DefaultJsonMapper.fromJson<PdlHendelseKafkaMelding>(melding.value())
            val saksnummer =
                behandlingRepository.finnSaksnummer(BehandlingReferanse(UUID.fromString(personHendelse.kildeReferanse)))
            val hendelseService = MottattHendelseService(repositoryProvider)
            hendelseService.registrerMottattHendelse(personHendelse.tilInnsending(saksnummer))

        }
    }

    private fun PdlHendelseKafkaMelding.tilInnsending(saksnummer: Saksnummer) =
        Innsending(
            saksnummer = saksnummer,
            referanse = InnsendingReferanse(PdlHendelseId(value = this.eventId)),
            type = InnsendingType.PDL_HENDELSE,
            kanal = Kanal.DIGITAL,
            mottattTidspunkt = LocalDateTime.now(),
            melding = this.tilPdlHendelseV0()
        )
}