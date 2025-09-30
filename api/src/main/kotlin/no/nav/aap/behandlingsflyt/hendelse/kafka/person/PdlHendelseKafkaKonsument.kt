package no.nav.aap.behandlingsflyt.hendelse.kafka.person

import no.nav.aap.behandlingsflyt.hendelse.mottak.MottattHendelseService
import no.nav.aap.behandlingsflyt.hendelse.kafka.KafkaConsumerConfig
import no.nav.aap.behandlingsflyt.hendelse.kafka.KafkaKonsument
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Endringstype
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Navn
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Opplysningstype
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.PdlPersonHendelse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.tilInnsending
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.db.PersonRepository
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.person.pdl.leesah.Personhendelse

import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.slf4j.LoggerFactory
import java.time.Duration
import javax.sql.DataSource

const val PDL_HENDELSE_TOPIC = "pdl.leesah-v1"

class PdlHendelseKafkaKonsument(
    config: KafkaConsumerConfig,
    pollTimeout: Duration = Duration.ofSeconds(10L),
    private val dataSource: DataSource,
    private val repositoryRegistry: RepositoryRegistry
) : KafkaKonsument<String, Personhendelse>(
    topic = PDL_HENDELSE_TOPIC,
    config = config,
    pollTimeout = pollTimeout,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun håndter(meldinger: ConsumerRecords<String, Personhendelse>) {
        meldinger.forEach { record ->
            val personHendelse = record.value().tilDomain()
            håndter(record, personHendelse)
        }
    }

    fun håndter(melding: ConsumerRecord<String, Personhendelse>, personHendelse: PdlPersonHendelse) {
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

    fun Personhendelse.tilDomain(): PdlPersonHendelse =
        PdlPersonHendelse(
            hendelseId = this.hendelseId,
            personidenter = this.personidenter,
            master = this.master,
            opprettet = this.opprettet,
            opplysningstype = Opplysningstype.valueOf(this.opplysningstype),
            endringstype = Endringstype.valueOf(this.endringstype.toString()),
            tidligereHendelseId = this.tidligereHendelseId,
            navn = this.navn?.let {
                Navn(
                    fornavn = it.fornavn,
                    etternavn = it.etternavn,
                    mellomnavn = it.mellomnavn,
                )
            }
        )
}