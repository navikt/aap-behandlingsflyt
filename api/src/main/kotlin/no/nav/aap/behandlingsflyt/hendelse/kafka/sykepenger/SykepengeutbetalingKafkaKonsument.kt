package no.nav.aap.behandlingsflyt.hendelse.kafka.sykepenger

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.sykepengemaksdato.MaksdatoHendelse
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.sykepengemaksdato.MaksdatoHendelseKilde
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.sykepengemaksdato.SykepengeMaksdatoRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.sykepengemaksdato.SykepengevedtakKafkaMelding
import no.nav.aap.behandlingsflyt.hendelse.kafka.KafkaConsumerConfig
import no.nav.aap.behandlingsflyt.hendelse.kafka.KafkaKonsument
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.IdentGateway
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.db.PersonRepository
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.komponenter.repository.RepositoryRegistry
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.slf4j.LoggerFactory
import javax.sql.DataSource
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

const val SYKEPENGEUTBETALING_EVENT_TOPIC = "tbd.utbetaling"

class SykepengeutbetalingKafkaKonsument(
    config: KafkaConsumerConfig<String, String>,
    pollTimeout: Duration = 10.seconds,
    closeTimeout: Duration = 30.seconds,
    private val dataSource: DataSource,
    private val repositoryRegistry: RepositoryRegistry,
    private val gatewayProvider: GatewayProvider,
) : KafkaKonsument<String, String>(
    topic = SYKEPENGEUTBETALING_EVENT_TOPIC,
    config = config,
    pollTimeout = pollTimeout,
    closeTimeout = closeTimeout,
    consumerName = "AapBehandlingsflytSykepengeutbetaling",
) {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun håndter(meldinger: ConsumerRecords<String, String>) {
        meldinger.forEach(::håndter)
    }

    private fun håndter(melding: ConsumerRecord<String, String>) {
        log.info(
            "Behandler sykepengeutbetaling-record, partition ${melding.partition()}, offset: ${melding.offset()}, topic: $topic"
        )
        håndter(melding.value())
    }

    private fun håndter(meldingVerdi: String) {
        dataSource.transaction { connection ->
            val repositoryProvider = repositoryRegistry.provider(connection)
            val personRepository: PersonRepository = repositoryProvider.provide()
            val sykepengeMaksdatoRepository: SykepengeMaksdatoRepository = repositoryProvider.provide()
            val sykepengeUtbetalingMelding = DefaultJsonMapper.fromJson<SykepengevedtakKafkaMelding>(meldingVerdi)
            val ident = Ident(sykepengeUtbetalingMelding.fødselsnummer)
            val sakRepository: SakRepository = repositoryProvider.provide()
            val person = personRepository.finn(ident) ?: finnPersonMedIdenterFraPdl(ident, personRepository)
            if (person != null && sakRepository.finnSakerFor(person).isNotEmpty()) {
                sykepengeMaksdatoRepository.lagre(
                    sykepengeUtbetalingMelding.tilMaksdatoHendelse(personRepository),
                    person
                )
            } else {
                log.info("Hopper over sykepengeutbetalinghendelse fordi person ikke har sak i Kelvin")
            }
        }

    }

    private fun finnPersonMedIdenterFraPdl(
        ident: Ident,
        personRepository: PersonRepository
    ): Person? {
        val pdlIdentGateway: IdentGateway = gatewayProvider.provide()
        val alleIdenter = pdlIdentGateway.hentAlleIdenterForPerson(ident)
        return personRepository.finn(alleIdenter)
    }

    private fun SykepengevedtakKafkaMelding.tilMaksdatoHendelse(personRepository: PersonRepository): MaksdatoHendelse {
        return MaksdatoHendelse(
            personId = requireNotNull(
                personRepository.finn(Ident(fødselsnummer))?.id ?: finnPersonMedIdenterFraPdl(
                    Ident(fødselsnummer),
                    personRepository
                )?.id
            ) {
                "Finner ikke personId for fødselsnummer på sykepengeutbetalinghendelse"
            },
            foreløpigMaksdato = foreløpigBeregnetSluttPåSykepenger,
            kilde = MaksdatoHendelseKilde.SPEIL,
        )
    }
}