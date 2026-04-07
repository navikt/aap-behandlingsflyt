package no.nav.aap.behandlingsflyt.hendelse.kafka.tilbakekreving

import no.nav.aap.behandlingsflyt.hendelse.kafka.KafkaConsumerConfig
import no.nav.aap.behandlingsflyt.hendelse.kafka.KafkaKonsument
import no.nav.aap.behandlingsflyt.hendelse.mottak.MottattHendelseService
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.FagsysteminfoBehovKafkaMelding
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.TilbakekrevingHendelseKafkaMelding
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.komponenter.miljo.Miljø
import no.nav.aap.komponenter.repository.RepositoryRegistry
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.slf4j.LoggerFactory
import javax.sql.DataSource
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

const val TILBAKEKREVING_EVENT_TOPIC = "tilbake.privat-tilbakekreving-arbeidsavklaringspenger"

class TilbakekrevingKafkaKonsument(
    config: KafkaConsumerConfig<String, String>,
    pollTimeout: Duration = 10.seconds,
    closeTimeout: Duration = 30.seconds,
    private val dataSource: DataSource,
    private val repositoryRegistry: RepositoryRegistry,
) : KafkaKonsument<String, String>(
    topic = TILBAKEKREVING_EVENT_TOPIC,
    config = config,
    pollTimeout = pollTimeout,
    closeTimeout = closeTimeout,
    consumerName = "AapBehandlingsflytTilbakekrevingHendelse",
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val secureLogger = LoggerFactory.getLogger("team-logs")

    override fun håndter(meldinger: ConsumerRecords<String, String>) {
        meldinger.forEach(::håndter)
    }

    fun håndter(melding: ConsumerRecord<String, String>) {
        log.info(
            "Behandler tilbakekreving-record med id: {}, partition {}, offset: {}",
            melding.key(),
            melding.partition(),
            melding.offset(),
        )
        melding.topic()
        val meldingKey = "${melding.partition()}-${melding.offset()}"
        håndter(meldingKey, melding.value())
    }

    fun håndter(meldingKey: String, meldingVerdi: String) {

        val tree = DefaultJsonMapper.objectMapper().readTree(meldingVerdi)
        val hendelsestype = tree["hendelsestype"]?.asText()
        val innsending = when (hendelsestype) {
            "behandling_endret" -> {
                val hendelse = try {
                    DefaultJsonMapper.fromJson<TilbakekrevingHendelseKafkaMelding>(meldingVerdi)
                } catch (exception: Exception) {
                    log.error("Kunne ikke parse melding fra tilbakekreving: $meldingKey", exception)
                    secureLogger.error("Kunne ikke parse melding fra tilbakekreving: $meldingKey med verdi: $meldingVerdi", exception)
                    throw exception
                }
                log.info("Mottatt tilbakekrevinghendelse for saksnummer: ${hendelse.eksternFagsakId}")
                hendelse.tilInnsending(meldingKey, Saksnummer(hendelse.eksternFagsakId))
            }
            "fagsysteminfo_behov" -> {
                val hendelse = try {
                    DefaultJsonMapper.fromJson<FagsysteminfoBehovKafkaMelding>(meldingVerdi)
                } catch (exception: Exception) {
                    log.error("Kunne ikke parse melding fra tilbakekreving: $meldingKey", exception)
                    secureLogger.error("Kunne ikke parse melding fra tilbakekreving: $meldingKey med verdi: $meldingVerdi", exception)
                    throw exception
                }
                log.info("Mottatt fagsysteminfobehovhendelse for saksnummer: ${hendelse.eksternFagsakId}")
                hendelse.tilInnsending(meldingKey, Saksnummer(hendelse.eksternFagsakId))
            }
            "fagsysteminfo_svar" -> {
                log.info("Hopper over meldinger med hendelsestype lik fagsysteminfo_svar")
                null
            }
            null -> {
                log.info("Hopper over meldinger med hendelsestype lik null: $meldingKey")
                secureLogger.info("Hopper over meldinger med hendelsestype lik null: $meldingVerdi")
                null
            }
            else -> {
                log.info("Hopper over meldinger med ukjent hendelsestype: $meldingKey")
                secureLogger.info("Hopper over meldinger med ukjent hendelsestype: $meldingVerdi")
                null
            }
        }
        if (innsending != null) {
            dataSource.transaction { connection ->
                val repositoryProvider = repositoryRegistry.provider(connection)
                val sakRepo = repositoryProvider.provide(SakRepository::class)
                val sak = sakRepo.hentHvisFinnes(innsending.saksnummer)
                //Sjekk at sak finnes. Kan skje i test/utv at det sendes hendelser på ikke-eksisterende saksnummer.
                if (sak == null) {
                    if (Miljø.erProd()) {
                        log.error("Fant ikke sak med saksnummer: ${innsending.saksnummer}")
                    } else {
                        log.info("Fant ikke sak med saksnummer: ${innsending.saksnummer}")
                    }
                } else {
                    val hendelseService = MottattHendelseService(repositoryProvider)
                    hendelseService.registrerMottattHendelse(innsending)
                }

            }
        }
    }

}
