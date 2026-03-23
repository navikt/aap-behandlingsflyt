package no.nav.aap.behandlingsflyt.behandling.meldekort

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import no.nav.aap.behandlingsflyt.BaseApiTest
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.ArbeidIPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Meldekort
import no.nav.aap.behandlingsflyt.integrasjon.createGatewayProvider
import no.nav.aap.behandlingsflyt.integrasjon.organisasjon.NomInfoGateway
import no.nav.aap.behandlingsflyt.integrasjon.organisasjon.NorgGateway
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.test.AlleAvskruddUnleash
import no.nav.aap.behandlingsflyt.test.Fakes
import no.nav.aap.behandlingsflyt.test.MockDataSource
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryMeldekortRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryVedtakRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.inMemoryRepositoryRegistry
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.TimerArbeid
import no.nav.aap.verdityper.dokument.JournalpostId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@Fakes
class MeldekortApiTest : BaseApiTest() {

    @Test
    fun `returnerer tomt sett når ingen vedtak finnes`() {
        val sak = nySak()
        opprettBehandling(sak, TypeBehandling.Førstegangsbehandling)

        testApplication {
            installApplication {
                meldekortApi(MockDataSource(), inMemoryRepositoryRegistry, createGatewayProvider {
                    register<NomInfoGateway>()
                    register<NorgGateway>()
                    register<AlleAvskruddUnleash>()
                })
            }

            val response = createClient().get("/api/meldekort/${sak.saksnummer}") {
                header("Authorization", "Bearer ${getToken().token()}")
            }

            assertThat(response.status).isEqualTo(HttpStatusCode.OK)
            val body = response.body<MeldekorteneDto>()
            assertThat(body.meldekortene).isEmpty()
        }
    }

    @Test
    fun `returnerer tomt sett når vedtak finnes men ingen meldekort`() {
        val sak = nySak()
        val behandling = opprettBehandling(sak, TypeBehandling.Førstegangsbehandling)

        InMemoryVedtakRepository.lagre(behandling.id, LocalDateTime.now(), LocalDate.now())

        testApplication {
            installApplication {
                meldekortApi(MockDataSource(), inMemoryRepositoryRegistry, createGatewayProvider {
                    register<NomInfoGateway>()
                    register<NorgGateway>()
                    register<AlleAvskruddUnleash>()
                })
            }

            val response = createClient().get("/api/meldekort/${sak.saksnummer}") {
                header("Authorization", "Bearer ${getToken().token()}")
            }

            assertThat(response.status).isEqualTo(HttpStatusCode.OK)
            val body = response.body<MeldekorteneDto>()
            assertThat(body.meldekortene).isEmpty()
        }
    }

    @Test
    fun `returnerer meldekort for siste fattede vedtak`() {
        val sak = nySak()
        val behandling = opprettBehandling(sak, TypeBehandling.Førstegangsbehandling)

        InMemoryVedtakRepository.lagre(behandling.id, LocalDateTime.now(), LocalDate.now())

        val dag1 = LocalDate.of(2025, 1, 6)
        val dag2 = LocalDate.of(2025, 1, 7)

        InMemoryMeldekortRepository.lagre(
            behandling.id, setOf(
                Meldekort(
                    journalpostId = JournalpostId("111"),
                    timerArbeidPerPeriode = setOf(
                        ArbeidIPeriode(Periode(dag1, dag1), TimerArbeid(BigDecimal("7.5"))),
                        ArbeidIPeriode(Periode(dag2, dag2), TimerArbeid(BigDecimal("3.0"))),
                    ),
                    mottattTidspunkt = LocalDateTime.of(2025, 1, 20, 9, 0)
                )
            )
        )

        testApplication {
            installApplication {
                meldekortApi(MockDataSource(), inMemoryRepositoryRegistry, createGatewayProvider {
                    register<NomInfoGateway>()
                    register<NorgGateway>()
                    register<AlleAvskruddUnleash>()
                })
            }

            val response = createClient().get("/api/meldekort/${sak.saksnummer}") {
                header("Authorization", "Bearer ${getToken().token()}")
            }

            assertThat(response.status).isEqualTo(HttpStatusCode.OK)
            val body = response.body<MeldekorteneDto>()
            assertThat(body.meldekortene).hasSize(1)

            val meldekort = body.meldekortene.first()
            assertThat(meldekort.meldekortId).isEqualTo("111")
            assertThat(meldekort.dager).hasSize(2)
            assertThat(meldekort.dager.map { it.dato }).containsExactlyInAnyOrder(dag1, dag2)
            assertThat(meldekort.dager.map { it.timerArbeidet }).containsExactlyInAnyOrder(7.5, 3.0)
        }
    }

    @Test
    fun `returnerer flere meldekort`() {
        val sak = nySak()
        val behandling = opprettBehandling(sak, TypeBehandling.Førstegangsbehandling)

        InMemoryVedtakRepository.lagre(behandling.id, LocalDateTime.now(), LocalDate.now())

        val dag = LocalDate.of(2025, 2, 3)

        InMemoryMeldekortRepository.lagre(
            behandling.id, setOf(
                Meldekort(
                    journalpostId = JournalpostId("aaa"),
                    timerArbeidPerPeriode = setOf(
                        ArbeidIPeriode(Periode(dag, dag), TimerArbeid(BigDecimal("4.0"))),
                    ),
                    mottattTidspunkt = LocalDateTime.of(2025, 2, 17, 9, 0)
                ),
                Meldekort(
                    journalpostId = JournalpostId("bbb"),
                    timerArbeidPerPeriode = setOf(
                        ArbeidIPeriode(Periode(dag.plusWeeks(2), dag.plusWeeks(2)), TimerArbeid(BigDecimal("6.0"))),
                    ),
                    mottattTidspunkt = LocalDateTime.of(2025, 3, 3, 9, 0)
                ),
            )
        )

        testApplication {
            installApplication {
                meldekortApi(MockDataSource(), inMemoryRepositoryRegistry, createGatewayProvider {
                    register<NomInfoGateway>()
                    register<NorgGateway>()
                    register<AlleAvskruddUnleash>()
                })
            }

            val response = createClient().get("/api/meldekort/${sak.saksnummer}") {
                header("Authorization", "Bearer ${getToken().token()}")
            }

            assertThat(response.status).isEqualTo(HttpStatusCode.OK)
            val body = response.body<MeldekorteneDto>()
            assertThat(body.meldekortene).hasSize(2)
            assertThat(body.meldekortene.map { it.meldekortId }).containsExactlyInAnyOrder("aaa", "bbb")
        }
    }
}

