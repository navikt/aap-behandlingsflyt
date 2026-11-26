package no.nav.aap.behandlingsflyt

import no.nav.aap.behandlingsflyt.help.FakePdlGateway
import no.nav.aap.behandlingsflyt.help.opprettInMemorySak
import no.nav.aap.behandlingsflyt.hendelse.kafka.uføre.UførevedtakMeldingsHåndterer
import no.nav.aap.behandlingsflyt.integrasjon.createGatewayProvider
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.UførevedtakKafkaMelding
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.UførevedtakResultat
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.UførevedtakV0
import no.nav.aap.behandlingsflyt.test.AlleAvskruddUnleash
import no.nav.aap.behandlingsflyt.test.MockDataSource
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryFlytJobbRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.inMemoryRepositoryRegistry
import no.nav.aap.komponenter.json.DefaultJsonMapper
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class UførevedtakMeldingsHåndtererTest {
    private val håndterer = UførevedtakMeldingsHåndterer(
        dataSource = MockDataSource(),
        repositoryRegistry = inMemoryRepositoryRegistry,
        gatewayProvider = createGatewayProvider {
            register<AlleAvskruddUnleash>()
            register<FakePdlGateway>()
        }
    )

    @Test
    fun `registrerer uførevedtak på personens sak med Kafka-referanse`() {
        val sak = opprettInMemorySak()
        val hendelse = UførevedtakKafkaMelding(
            personId = sak.person.aktivIdent().identifikator,
            virkningsdato = LocalDate.of(2026, 1, 15),
            resultat = UførevedtakResultat.AVSL,
            avslag12_5 = true
        )

        håndterer.håndter(
            ConsumerRecord("uførevedtak", 2, 42L, "nøkkel", DefaultJsonMapper.toJson(hendelse))
        )

        val jobber = InMemoryFlytJobbRepository.hentJobberForSak(sak.id.id)
        assertThat(jobber).hasSize(1)
        val jobb = jobber.single()
        assertThat(jobb.parameter("brevkode")).isEqualTo(InnsendingType.UFØRE_VEDTAK_HENDELSE.name)
        assertThat(DefaultJsonMapper.fromJson<InnsendingReferanse>(jobb.parameter("referanse")))
            .isEqualTo(hendelse.tilInnsending("2-42", sak.saksnummer).referanse)
        assertThat(DefaultJsonMapper.fromJson<UførevedtakV0>(jobb.payload()))
            .isEqualTo(hendelse.tilUføreVedtakV0())
    }
}
