package no.nav.aap.behandlingsflyt.hendelse.kafka.person

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonopplysningRepository
import no.nav.aap.behandlingsflyt.hendelse.mottak.MottattHendelseService
import no.nav.aap.behandlingsflyt.hendelse.kafka.KafkaConsumerConfig
import no.nav.aap.behandlingsflyt.hendelse.kafka.KafkaKonsument
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.PdlHendelseId
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Endringstype
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Innsending
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Opplysningstype
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.PdlHendelse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Personhendelse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.innsendingType
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.tilInnsending
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.kontrakt.sak.Status
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.db.PersonRepository
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
            val sakRepository: SakRepository = repositoryProvider.provide()
            val personRepository: PersonRepository = repositoryProvider.provide()
            val personHendelse = DefaultJsonMapper.fromJson<Personhendelse>(melding.value())
            val hendelseService = MottattHendelseService(repositoryProvider)
            log.info("Leser personhendelse $personHendelse")
            if (personHendelse.opplysningstype == Opplysningstype.AVDOED_PDL_V1) {
                log.info("Håndterer hendelse med ${personHendelse.opplysningstype} og ${personHendelse.endringstype}")
                personHendelse.personidenter
                    .mapNotNull { ident -> personRepository.finn(Ident(ident)) }
                    .forEach { person ->
                        sakRepository.finnSakerFor(person).forEach { sak ->
                            hendelseService.registrerMottattHendelse(
                                personHendelse.tilInnsending(sak.saksnummer)
                            )
                        }
                    }
            } else {
                log.info("Ignorerer hendelse med ${personHendelse.opplysningstype} og ${personHendelse.endringstype}")
            }
        }
    }

}