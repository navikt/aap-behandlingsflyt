package no.nav.aap.behandlingsflyt.hendelse.kafka.person

import no.nav.aap.behandlingsflyt.hendelse.kafka.KafkaConsumerConfig
import no.nav.aap.behandlingsflyt.hendelse.kafka.KafkaKonsument
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Endringstype
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Navn
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Opplysningstype
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.PdlPersonHendelse
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.person.pdl.leesah.Personhendelse
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.slf4j.LoggerFactory
import javax.sql.DataSource
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

const val PDL_HENDELSE_TOPIC = "pdl.leesah-v1"

class PdlHendelseKafkaKonsument(
    config: KafkaConsumerConfig<String, Personhendelse>,
    pollTimeout: Duration = 10.seconds,
    closeTimeout: Duration = 30.seconds,
    private val dataSource: DataSource,
    private val repositoryRegistry: RepositoryRegistry,
    private val gatewayProvider: GatewayProvider,
) : KafkaKonsument<String, Personhendelse>(
    consumerName = "PdlHendelse",
    topic = PDL_HENDELSE_TOPIC,
    config = config,
    pollTimeout = pollTimeout,
    closeTimeout = closeTimeout,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    override fun håndter(meldinger: ConsumerRecords<String, Personhendelse>) {
        meldinger.forEach { record ->
            val personHendelse = record.value().tilDomain()
            dataSource.transaction { connection ->
                PdlHendelseService(repositoryRegistry.provider(connection), gatewayProvider).håndter(personHendelse)
            }
        }
    }

    fun Personhendelse.tilDomain(): PdlPersonHendelse =
        PdlPersonHendelse(
            hendelseId = this.hendelseId,
            personidenter = this.personidenter,
            master = this.master,
            opprettet = this.opprettet,
            opplysningstype = try {
                Opplysningstype.valueOf(this.opplysningstype)
            } catch (_: IllegalArgumentException) {
                log.info("Fant ukjent opplysningstype fra PDL: ${this.opplysningstype}")
                Opplysningstype.UNKNOWN
            },
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

    enum class Dødsfalltype(val verdi: String) {
        DODSFALL_BRUKER("DODSFALL_BRUKER"), DODSFALL_BARN("DODSFALL_BARN")
    }
}