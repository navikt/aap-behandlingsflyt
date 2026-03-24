package no.nav.aap.behandlingsflyt.behandling.meldekort

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import no.nav.aap.behandlingsflyt.BaseApiTest
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.MeldepliktStatus
import no.nav.aap.behandlingsflyt.faktagrunnlag.Faktagrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.ArbeidsGradering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.Underveisperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisÅrsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
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
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryUnderveisRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryVedtakRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.inMemoryRepositoryRegistry
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Dagsatser
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.komponenter.verdityper.Prosent.Companion.`0_PROSENT`
import no.nav.aap.komponenter.verdityper.TimerArbeid
import no.nav.aap.verdityper.dokument.JournalpostId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

@Fakes
class MeldekortApiTest : BaseApiTest() {

    private val fixedClock = Clock.fixed(Instant.parse("2025-04-01T00:00:00Z"), ZoneId.systemDefault())

    private fun createTestGatewayProvider() = createGatewayProvider {
        register<NomInfoGateway>()
        register<NorgGateway>()
        register<AlleAvskruddUnleash>()
    }

    @Test
    fun `returnerer tomt sett naar ingen vedtak finnes`() {
        val sak = nySak()
        opprettBehandling(sak, TypeBehandling.Førstegangsbehandling)

        testApplication {
            installApplication {
                meldekortApi(MockDataSource(), inMemoryRepositoryRegistry, createTestGatewayProvider(), fixedClock)
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
    fun `returnerer tom meldekort for meldeperiode uten innsendt meldekort`() {
        val sak = nySak()
        val behandling = opprettBehandling(sak, TypeBehandling.Førstegangsbehandling)

        InMemoryVedtakRepository.lagre(behandling.id, LocalDateTime.now(), LocalDate.now())

        val meldeperiode = Periode(LocalDate.of(2025, 1, 6), LocalDate.of(2025, 1, 19))

        InMemoryUnderveisRepository.lagre(
            behandlingId = behandling.id,
            underveisperioder = listOf(underveisperiode(Utfall.OPPFYLT, meldeperiode)),
            input = object : Faktagrunnlag {}
        )

        testApplication {
            installApplication {
                meldekortApi(MockDataSource(), inMemoryRepositoryRegistry, createTestGatewayProvider(), fixedClock)
            }

            val response = createClient().get("/api/meldekort/${sak.saksnummer}") {
                header("Authorization", "Bearer ${getToken().token()}")
            }

            assertThat(response.status).isEqualTo(HttpStatusCode.OK)
            val body = response.body<MeldekorteneDto>()
            assertThat(body.meldekortene).hasSize(1)

            val meldekort = body.meldekortene.first()
            assertThat(meldekort.id).isNull()
            assertThat(meldekort.meldeperiode).isEqualTo(meldeperiode)
            assertThat(meldekort.mottattTidspunkt).isNull()
            assertThat(meldekort.dager).isEmpty()
        }
    }

    @Test
    fun `returnerer meldekort for siste fattede vedtak`() {
        val sak = nySak()
        val behandling = opprettBehandling(sak, TypeBehandling.Førstegangsbehandling)

        InMemoryVedtakRepository.lagre(behandling.id, LocalDateTime.now(), LocalDate.now())

        val dag1 = LocalDate.of(2025, 1, 6)
        val dag2 = LocalDate.of(2025, 1, 7)
        val meldeperiode = Periode(dag1, dag1.plusDays(13))

        InMemoryUnderveisRepository.lagre(
            behandlingId = behandling.id,
            underveisperioder = listOf(underveisperiode(Utfall.OPPFYLT, meldeperiode)),
            input = object : Faktagrunnlag {}
        )

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
                meldekortApi(MockDataSource(), inMemoryRepositoryRegistry, createTestGatewayProvider(), fixedClock)
            }

            val response = createClient().get("/api/meldekort/${sak.saksnummer}") {
                header("Authorization", "Bearer ${getToken().token()}")
            }

            assertThat(response.status).isEqualTo(HttpStatusCode.OK)
            val body = response.body<MeldekorteneDto>()
            assertThat(body.meldekortene).hasSize(1)

            val meldekort = body.meldekortene.first()
            assertThat(meldekort.id).isEqualTo("111")
            assertThat(meldekort.meldeperiode).isEqualTo(meldeperiode)
            assertThat(meldekort.mottattTidspunkt).isEqualTo(LocalDateTime.of(2025, 1, 20, 9, 0))
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
        val meldeperiode1 = Periode(dag, dag.plusDays(13))
        val meldeperiode2 = Periode(dag.plusWeeks(2), dag.plusWeeks(2).plusDays(13))

        InMemoryUnderveisRepository.lagre(
            behandlingId = behandling.id,
            underveisperioder = listOf(
                underveisperiode(Utfall.OPPFYLT, meldeperiode1),
                underveisperiode(Utfall.OPPFYLT, meldeperiode2),
            ),
            input = object : Faktagrunnlag {}
        )

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
                meldekortApi(MockDataSource(), inMemoryRepositoryRegistry, createTestGatewayProvider(), fixedClock)
            }

            val response = createClient().get("/api/meldekort/${sak.saksnummer}") {
                header("Authorization", "Bearer ${getToken().token()}")
            }

            assertThat(response.status).isEqualTo(HttpStatusCode.OK)
            val body = response.body<MeldekorteneDto>()
            assertThat(body.meldekortene).hasSize(2)
            assertThat(body.meldekortene.map { it.id }).containsExactlyInAnyOrder("aaa", "bbb")
        }
    }

    @Test
    fun `ekskluderer meldeperioder med tom-dato etter nå`() {
        val sak = nySak()
        val behandling = opprettBehandling(sak, TypeBehandling.Førstegangsbehandling)

        InMemoryVedtakRepository.lagre(behandling.id, LocalDateTime.now(), LocalDate.now())

        // fixedClock = 2025-04-01
        val pastPeriode = Periode(LocalDate.of(2025, 3, 3), LocalDate.of(2025, 3, 16))    // tom < now → included
        val futurePeriode = Periode(LocalDate.of(2025, 3, 31), LocalDate.of(2025, 4, 13)) // tom > now → excluded

        InMemoryUnderveisRepository.lagre(
            behandlingId = behandling.id,
            underveisperioder = listOf(
                underveisperiode(Utfall.OPPFYLT, pastPeriode),
                underveisperiode(Utfall.OPPFYLT, futurePeriode),
            ),
            input = object : Faktagrunnlag {}
        )

        testApplication {
            installApplication {
                meldekortApi(MockDataSource(), inMemoryRepositoryRegistry, createTestGatewayProvider(), fixedClock)
            }

            val response = createClient().get("/api/meldekort/${sak.saksnummer}") {
                header("Authorization", "Bearer ${getToken().token()}")
            }

            assertThat(response.status).isEqualTo(HttpStatusCode.OK)
            val body = response.body<MeldekorteneDto>()
            assertThat(body.meldekortene).hasSize(1)
            assertThat(body.meldekortene.first().meldeperiode).isEqualTo(pastPeriode)
        }
    }

    private fun underveisperiode(utfall: Utfall, periode: Periode) = Underveisperiode(
        periode = periode,
        meldePeriode = periode,
        utfall = utfall,
        rettighetsType = RettighetsType.BISTANDSBEHOV,
        avslagsårsak = if (utfall == Utfall.IKKE_OPPFYLT) UnderveisÅrsak.BRUDD_PÅ_AKTIVITETSPLIKT_11_7_STANS else null,
        grenseverdi = Prosent.`100_PROSENT`,
        arbeidsgradering = ArbeidsGradering(
            totaltAntallTimer = TimerArbeid(BigDecimal.ZERO),
            andelArbeid = `0_PROSENT`,
            fastsattArbeidsevne = Prosent.`100_PROSENT`,
            gradering = Prosent.`100_PROSENT`,
            opplysningerMottatt = null,
        ),
        trekk = Dagsatser(0),
        brukerAvKvoter = emptySet(),
        institusjonsoppholdReduksjon = `0_PROSENT`,
        meldepliktStatus = MeldepliktStatus.MELDT_SEG,
        meldepliktGradering = `0_PROSENT`,
    )
}
