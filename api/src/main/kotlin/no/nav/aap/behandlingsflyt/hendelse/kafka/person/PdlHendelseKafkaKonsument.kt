package no.nav.aap.behandlingsflyt.hendelse.kafka.person

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonopplysningRepository
import no.nav.aap.behandlingsflyt.hendelse.kafka.KafkaConsumerConfig
import no.nav.aap.behandlingsflyt.hendelse.kafka.KafkaKonsument
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.PdlHendelseKafkaMelding
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.komponenter.repository.RepositoryRegistry
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.slf4j.LoggerFactory
import java.time.Duration
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
            val personopplysningRepository: PersonopplysningRepository = repositoryProvider.provide()
            val personHendelse = DefaultJsonMapper.fromJson<PdlHendelseKafkaMelding>(melding.value())
            val saksnummer =
                behandlingRepository.finnSaksnummer(BehandlingReferanse(UUID.fromString(personHendelse.doedsdato.toString())))
            //          personopplysningRepository.lagre(behandlingId = behandlingId, personopplysning = personopplysning )
            //TODO lagrre nye personopplysninger
            /*
            type Doedsfall {
                 doedsdato: Date
                metadata: Metadata!
                 folkeregistermetadata: Folkeregistermetadata
}

             */
        }
    }

}