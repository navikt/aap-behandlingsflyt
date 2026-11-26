package no.nav.aap.behandlingsflyt

import no.nav.aap.behandlingsflyt.help.FakePdlGateway
import no.nav.aap.behandlingsflyt.help.opprettInMemorySakOgBehandling
import no.nav.aap.behandlingsflyt.hendelse.kafka.sykepenger.SYKEPENGEVEDTAK_EVENT_TOPIC
import no.nav.aap.behandlingsflyt.hendelse.kafka.sykepenger.SykepengevedtakMeldingsHåndterer
import no.nav.aap.behandlingsflyt.integrasjon.createGatewayProvider
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.SykepengevedtakKafkaMelding
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.SykepengevedtakV0
import no.nav.aap.behandlingsflyt.test.AlleAvskruddUnleash
import no.nav.aap.behandlingsflyt.test.MockDataSource
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryFlytJobbRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.inMemoryRepositoryRegistry
import no.nav.aap.komponenter.json.DefaultJsonMapper
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime

class SykepengevedtakMeldingsHåndtererTest {
    private val håndterer = SykepengevedtakMeldingsHåndterer(
        dataSource = MockDataSource(),
        repositoryRegistry = inMemoryRepositoryRegistry,
        gatewayProvider = createGatewayProvider {
            register<AlleAvskruddUnleash>()
            register<FakePdlGateway>()
        }
    )

    @Test
    fun `registrerer sykepengehendelse på personens sak med Kafka-referanse`() {
        val (sak, _) = opprettInMemorySakOgBehandling()
        val hendelse = SykepengevedtakKafkaMelding(
            personidentifikator = sak.person.aktivIdent().identifikator,
            tidspunkt = OffsetDateTime.parse("2026-01-15T12:00:00Z")
        )

        håndterer.håndter(
            ConsumerRecord(SYKEPENGEVEDTAK_EVENT_TOPIC, 2, 42L, "nøkkel", DefaultJsonMapper.toJson(hendelse))
        )

        val jobber = InMemoryFlytJobbRepository.hentJobberForSak(sak.id.id)
        assertThat(jobber).hasSize(1)
        val jobb = jobber.single()
        assertThat(jobb.parameter("brevkode")).isEqualTo(InnsendingType.SYKEPENGE_VEDTAK_HENDELSE.name)
        assertThat(DefaultJsonMapper.fromJson<InnsendingReferanse>(jobb.parameter("referanse")))
            .isEqualTo(hendelse.tilInnsending("2-42", sak.saksnummer).referanse)
        assertThat(DefaultJsonMapper.fromJson<SykepengevedtakV0>(jobb.payload()))
            .usingRecursiveComparison()
            .withComparatorForType(Comparator.comparing(OffsetDateTime::toInstant), OffsetDateTime::class.java)
            .isEqualTo(hendelse.tilSykepengeSpleisVedtakV0())
    }
}
