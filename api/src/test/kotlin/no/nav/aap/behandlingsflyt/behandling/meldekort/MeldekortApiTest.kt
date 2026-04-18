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
import no.nav.aap.behandlingsflyt.test.FakeDokarkivGateway
import no.nav.aap.behandlingsflyt.test.Fakes
import no.nav.aap.behandlingsflyt.test.MockDataSource
import no.nav.aap.behandlingsflyt.test.april
import no.nav.aap.behandlingsflyt.test.februar
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryMeldekortRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryUnderveisRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryVedtakRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.inMemoryRepositoryRegistry
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.behandlingsflyt.test.mars
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

    private fun createTestGatewayProviderMedDokarkiv() = createGatewayProvider {
        register<NomInfoGateway>()
        register<NorgGateway>()
        register<AlleAvskruddUnleash>()
        register<FakeDokarkivGateway>()
    }

    @Test
    fun `returnerer tomt sett når ingen vedtak finnes`() {
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
            val body = response.body<MeldeperioderMedMeldekortResponse>()
            assertThat(body.meldeperioderMedMeldekort).isEmpty()
        }
    }

    @Test
    fun `returnerer tomt meldekort for meldeperiode uten innsendt meldekort`() {
        val sak = nySak()
        val behandling = opprettBehandling(sak, TypeBehandling.Førstegangsbehandling)

        InMemoryVedtakRepository.lagre(behandling.id, LocalDateTime.now(), LocalDate.now())

        val meldeperiode = Periode(6 januar 2025, 19 januar 2025)

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
            val body = response.body<MeldeperioderMedMeldekortResponse>()
            assertThat(body.meldeperioderMedMeldekort).hasSize(1)

            val meldeperiodeMedMeldekort = body.meldeperioderMedMeldekort.first()
            assertThat(meldeperiodeMedMeldekort.meldeperiode).isEqualTo(meldeperiode)
            assertThat(meldeperiodeMedMeldekort.meldekort).isNull()
        }
    }

    @Test
    fun `returnerer meldekort for siste fattede vedtak`() {
        val sak = nySak()
        val behandling = opprettBehandling(sak, TypeBehandling.Førstegangsbehandling)

        InMemoryVedtakRepository.lagre(behandling.id, LocalDateTime.now(), LocalDate.now())

        val dag1 = 6 januar 2025
        val dag2 = 7 januar 2025
        val meldeperiode = Periode(dag1, dag1.plusDays(13))
        val meldekort = Meldekort(
            journalpostId = JournalpostId("111"),
            timerArbeidPerPeriode = setOf(
                ArbeidIPeriode(Periode(dag1, dag1), TimerArbeid(BigDecimal("7.5"))),
                ArbeidIPeriode(Periode(dag2, dag2), TimerArbeid(BigDecimal("3.0"))),
            ),
            mottattTidspunkt = LocalDateTime.of(2025, 1, 20, 9, 0)
        )

        InMemoryUnderveisRepository.lagre(
            behandlingId = behandling.id,
            underveisperioder = listOf(underveisperiode(Utfall.OPPFYLT, meldeperiode)),
            input = object : Faktagrunnlag {}
        )

        InMemoryMeldekortRepository.lagre(
            behandling.id, setOf(meldekort)
        )

        testApplication {
            installApplication {
                meldekortApi(MockDataSource(), inMemoryRepositoryRegistry, createTestGatewayProvider(), fixedClock)
            }

            val response = createClient().get("/api/meldekort/${sak.saksnummer}") {
                header("Authorization", "Bearer ${getToken().token()}")
            }

            assertThat(response.status).isEqualTo(HttpStatusCode.OK)
            val body = response.body<MeldeperioderMedMeldekortResponse>()
            assertThat(body.meldeperioderMedMeldekort).hasSize(1)

            val meldeperiodeMedMeldekort = body.meldeperioderMedMeldekort.first()
            assertThat(meldeperiodeMedMeldekort.meldeperiode).isEqualTo(meldeperiode)
            assertThat(meldeperiodeMedMeldekort.meldekort).isNotNull
            assertThat(meldeperiodeMedMeldekort.meldekort!!.id).isEqualTo(meldekort.journalpostId.identifikator)
            assertThat(meldeperiodeMedMeldekort.meldekort.mottattTidspunkt).isEqualTo(meldekort.mottattTidspunkt)
            assertThat(meldeperiodeMedMeldekort.meldekort.dager).hasSize(2)
            assertThat(meldeperiodeMedMeldekort.meldekort.dager.map { it.dato }).containsExactlyInAnyOrder(dag1, dag2)
            assertThat(meldeperiodeMedMeldekort.meldekort.dager.map { it.timerArbeidet }).containsExactlyInAnyOrder(7.5, 3.0)
        }
    }

    @Test
    fun `returnerer flere meldekort`() {
        val sak = nySak()
        val behandling = opprettBehandling(sak, TypeBehandling.Førstegangsbehandling)

        InMemoryVedtakRepository.lagre(behandling.id, LocalDateTime.now(), LocalDate.now())

        val dag = 3 februar 2025
        val meldeperiode1 = Periode(dag, dag.plusDays(13))
        val meldeperiode2 = Periode(dag.plusWeeks(2), dag.plusWeeks(2).plusDays(13))
        val meldekort1 = Meldekort(
            journalpostId = JournalpostId("aaa"),
            timerArbeidPerPeriode = setOf(
                ArbeidIPeriode(Periode(dag, dag), TimerArbeid(BigDecimal("4.0"))),
            ),
            mottattTidspunkt = LocalDateTime.of(2025, 2, 17, 9, 0)
        )
        val meldekort2 = Meldekort(
            journalpostId = JournalpostId("bbb"),
            timerArbeidPerPeriode = setOf(
                ArbeidIPeriode(Periode(dag.plusWeeks(2), dag.plusWeeks(2)), TimerArbeid(BigDecimal("6.0"))),
            ),
            mottattTidspunkt = LocalDateTime.of(2025, 3, 3, 9, 0)
        )

        InMemoryUnderveisRepository.lagre(
            behandlingId = behandling.id,
            underveisperioder = listOf(
                underveisperiode(Utfall.OPPFYLT, meldeperiode1),
                underveisperiode(Utfall.OPPFYLT, meldeperiode2),
            ),
            input = object : Faktagrunnlag {}
        )

        InMemoryMeldekortRepository.lagre(
            behandling.id, setOf(meldekort1, meldekort2)
        )

        testApplication {
            installApplication {
                meldekortApi(MockDataSource(), inMemoryRepositoryRegistry, createTestGatewayProvider(), fixedClock)
            }

            val response = createClient().get("/api/meldekort/${sak.saksnummer}") {
                header("Authorization", "Bearer ${getToken().token()}")
            }

            assertThat(response.status).isEqualTo(HttpStatusCode.OK)
            val body = response.body<MeldeperioderMedMeldekortResponse>()
            assertThat(body.meldeperioderMedMeldekort).hasSize(2)
            assertThat(body.meldeperioderMedMeldekort.mapNotNull { it.meldekort?.id })
                .containsExactlyInAnyOrder(meldekort1.journalpostId.identifikator, meldekort2.journalpostId.identifikator)
        }
    }

    @Test
    fun `returnerer korrigert meldekort når flere meldekort finnes for samme periode`() {
        val sak = nySak()
        val behandling = opprettBehandling(sak, TypeBehandling.Førstegangsbehandling)

        InMemoryVedtakRepository.lagre(behandling.id, LocalDateTime.now(), LocalDate.now())

        val dag1 = 6 januar 2025
        val meldeperiode = Periode(dag1, dag1.plusDays(13))

        val opprinneligMeldekort = Meldekort(
            journalpostId = JournalpostId("111"),
            timerArbeidPerPeriode = setOf(
                ArbeidIPeriode(Periode(dag1, dag1), TimerArbeid(BigDecimal("7.5"))),
            ),
            mottattTidspunkt = LocalDateTime.of(2025, 1, 20, 9, 0)
        )
        val korrigertMeldekort = Meldekort(
            journalpostId = JournalpostId("222"),
            timerArbeidPerPeriode = setOf(
                ArbeidIPeriode(Periode(dag1, dag1), TimerArbeid(BigDecimal.ZERO)),
            ),
            mottattTidspunkt = LocalDateTime.of(2025, 1, 25, 9, 0)
        )

        InMemoryUnderveisRepository.lagre(
            behandlingId = behandling.id,
            underveisperioder = listOf(underveisperiode(Utfall.OPPFYLT, meldeperiode)),
            input = object : Faktagrunnlag {}
        )

        InMemoryMeldekortRepository.lagre(
            behandling.id, setOf(opprinneligMeldekort, korrigertMeldekort)
        )

        testApplication {
            installApplication {
                meldekortApi(MockDataSource(), inMemoryRepositoryRegistry, createTestGatewayProvider(), fixedClock)
            }

            val response = createClient().get("/api/meldekort/${sak.saksnummer}") {
                header("Authorization", "Bearer ${getToken().token()}")
            }

            assertThat(response.status).isEqualTo(HttpStatusCode.OK)
            val body = response.body<MeldeperioderMedMeldekortResponse>()
            assertThat(body.meldeperioderMedMeldekort).hasSize(1)

            val meldeperiodeMedMeldekort = body.meldeperioderMedMeldekort.first()
            assertThat(meldeperiodeMedMeldekort.meldekort).isNotNull
            assertThat(meldeperiodeMedMeldekort.meldekort!!.id).isEqualTo(korrigertMeldekort.journalpostId.identifikator)
            assertThat(meldeperiodeMedMeldekort.meldekort.mottattTidspunkt).isEqualTo(korrigertMeldekort.mottattTidspunkt)
            assertThat(meldeperiodeMedMeldekort.meldekort.dager).hasSize(1)
            assertThat(meldeperiodeMedMeldekort.meldekort.dager.first().timerArbeidet).isEqualTo(0.0)
        }
    }

    @Test
    fun `returnerer ikke meldeperioder med fom-dato frem i tid`() {
        val sak = nySak()
        val behandling = opprettBehandling(sak, TypeBehandling.Førstegangsbehandling)

        InMemoryVedtakRepository.lagre(behandling.id, LocalDateTime.now(), LocalDate.now())

        // fixedClock = 2025-04-01
        val forrigeMeldeperiode = Periode(15 mars 2025, 29 mars 2025) // skal være med
        val inneværendeMeldeperiode = Periode(30 mars 2025, 12 april 2025) // skal være med
        val fremtidigMeldeperiode = Periode(13 april 2025, 27 april 2025) // skal ikke være med

        InMemoryUnderveisRepository.lagre(
            behandlingId = behandling.id,
            underveisperioder = listOf(
                underveisperiode(Utfall.OPPFYLT, forrigeMeldeperiode),
                underveisperiode(Utfall.OPPFYLT, inneværendeMeldeperiode),
                underveisperiode(Utfall.OPPFYLT, fremtidigMeldeperiode),
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
            val body = response.body<MeldeperioderMedMeldekortResponse>()
            assertThat(body.meldeperioderMedMeldekort).hasSize(2)
            assertThat(body.meldeperioderMedMeldekort.first().meldeperiode).isEqualTo(forrigeMeldeperiode)
            assertThat(body.meldeperioderMedMeldekort.last().meldeperiode).isEqualTo(inneværendeMeldeperiode)
        }
    }

    @Test
    fun `skal journalføre oppdatert meldekort og returnere journalpostId`() {
        val sak = nySak()
        val behandling = opprettBehandling(sak, TypeBehandling.Førstegangsbehandling)

        InMemoryVedtakRepository.lagre(behandling.id, LocalDateTime.now(), LocalDate.now())

        val dag1 = 6 januar 2025
        val dag2 = 7 januar 2025

        val request = OppdaterMeldekortRequest(
            saksnummer = sak.saksnummer.toString(),
            begrunnelse = "Korrigering av timer",
            dager = setOf(
                DagDto(dato = dag1, timerArbeidet = 7.5),
                DagDto(dato = dag2, timerArbeidet = 3.0),
            ),
        )

        testApplication {
            installApplication {
                meldekortApi(MockDataSource(), inMemoryRepositoryRegistry, createTestGatewayProviderMedDokarkiv(), fixedClock)
            }

            val response = createClient().post("/api/meldekort/oppdater") {
                header("Authorization", "Bearer ${getToken().token()}")
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            assertThat(response.status).isEqualTo(HttpStatusCode.OK)
            val body = response.body<OppdaterMeldekortResponse>()
            assertThat(body.journalpostId).isNotBlank()
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
