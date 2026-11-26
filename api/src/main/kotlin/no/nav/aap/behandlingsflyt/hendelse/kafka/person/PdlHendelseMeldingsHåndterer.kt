package no.nav.aap.behandlingsflyt.hendelse.kafka.person

import no.nav.aap.behandlingsflyt.hendelse.kafka.KafkaMeldingsHåndterer
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Endringstype
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Navn
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Opplysningstype
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.PdlPersonHendelse
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.person.pdl.leesah.Personhendelse
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import javax.sql.DataSource

const val PDL_HENDELSE_TOPIC = "pdl.leesah-v1"

class PdlHendelseMeldingsHåndterer(
    private val dataSource: DataSource,
    private val repositoryRegistry: RepositoryRegistry,
    private val gatewayProvider: GatewayProvider
) : KafkaMeldingsHåndterer<String, Personhendelse> {
    override val navn = "PdlHendelse"

    init {
        // Avro-sikkerhet, trengs for å unngå java.lang.SecurityException
        System.setProperty(
            "org.apache.avro.SERIALIZABLE_PACKAGES",
            "no.nav.person"
        )
    }

    private val log = LoggerFactory.getLogger(javaClass)
    override fun håndter(melding: ConsumerRecord<String, Personhendelse>) {
        val personHendelse = melding.value().tilDomain()
        dataSource.transaction { connection ->
            PdlHendelseService(repositoryRegistry.provider(connection), gatewayProvider).håndter(personHendelse)
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