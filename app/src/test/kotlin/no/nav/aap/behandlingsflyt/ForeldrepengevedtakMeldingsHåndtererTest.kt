package no.nav.aap.behandlingsflyt

import no.nav.aap.behandlingsflyt.help.FakePdlGateway
import no.nav.aap.behandlingsflyt.help.opprettInMemorySakOgBehandling
import no.nav.aap.behandlingsflyt.hendelse.kafka.foreldrepenger.FORELDREPENGEVEDTAK_EVENT_TOPIC
import no.nav.aap.behandlingsflyt.hendelse.kafka.foreldrepenger.ForeldrepengevedtakMeldingsHåndterer
import no.nav.aap.behandlingsflyt.integrasjon.createGatewayProvider
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.ForeldrepengeVedtakV0
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.ForeldrepengevedtakKafkaMelding
import no.nav.aap.behandlingsflyt.test.AlleAvskruddUnleash
import no.nav.aap.behandlingsflyt.test.MockDataSource
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryFlytJobbRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.inMemoryRepositoryRegistry
import no.nav.aap.komponenter.json.DefaultJsonMapper
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime

class ForeldrepengevedtakMeldingsHåndtererTest {
    private val håndterer = ForeldrepengevedtakMeldingsHåndterer(
        dataSource = MockDataSource(),
        repositoryRegistry = inMemoryRepositoryRegistry,
        gatewayProvider = createGatewayProvider {
            register<AlleAvskruddUnleash>()
            register<FakePdlGateway>()
        }
    )

    @Test
    fun `registrerer foreldrepengehendelse på personens sak med Kafka-referanse`() {
        val (sak, _) = opprettInMemorySakOgBehandling()
        val hendelse = ForeldrepengevedtakKafkaMelding(
            personidentifikator = sak.person.aktivIdent().identifikator,
            tidspunkt = OffsetDateTime.parse("2026-01-15T12:00:00Z"),
            tema = "FOR"
        )

        håndterer.håndter(
            ConsumerRecord(FORELDREPENGEVEDTAK_EVENT_TOPIC, 2, 42L, "nøkkel", DefaultJsonMapper.toJson(hendelse))
        )

        val jobber = InMemoryFlytJobbRepository.hentJobberForSak(sak.id.id)
        assertThat(jobber).hasSize(1)
        val jobb = jobber.single()
        assertThat(jobb.parameter("brevkode")).isEqualTo(InnsendingType.FORELDREPENGE_VEDTAK_HENDELSE.name)
        assertThat(DefaultJsonMapper.fromJson<InnsendingReferanse>(jobb.parameter("referanse")))
            .isEqualTo(hendelse.tilInnsending("2-42", sak.saksnummer).referanse)
        assertThat(DefaultJsonMapper.fromJson<ForeldrepengeVedtakV0>(jobb.payload()))
            .usingRecursiveComparison()
            .withComparatorForType(Comparator.comparing(OffsetDateTime::toInstant), OffsetDateTime::class.java)
            .isEqualTo(hendelse.tilForeldrepengeVedtakV0())
    }
}
