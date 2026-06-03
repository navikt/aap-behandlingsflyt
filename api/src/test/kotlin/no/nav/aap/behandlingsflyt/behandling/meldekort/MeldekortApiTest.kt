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
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokument
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.StrukturertDokument
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.ArbeidIPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Meldekort
import no.nav.aap.behandlingsflyt.integrasjon.createGatewayProvider
import no.nav.aap.behandlingsflyt.integrasjon.organisasjon.NomInfoGateway
import no.nav.aap.behandlingsflyt.integrasjon.organisasjon.NorgGateway
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.ArbeidIPeriodeV0
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.MeldekortV0
import no.nav.aap.behandlingsflyt.prosessering.HendelseMottattHåndteringJobbUtfører
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.test.AlleAvskruddUnleash
import no.nav.aap.behandlingsflyt.test.FakeDokarkivGateway
import no.nav.aap.behandlingsflyt.test.FakePdfgenGateway
import no.nav.aap.behandlingsflyt.test.Fakes
import no.nav.aap.behandlingsflyt.test.MockDataSource
import no.nav.aap.behandlingsflyt.test.april
import no.nav.aap.behandlingsflyt.test.februar
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryFlytJobbRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryMeldekortRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryMottattDokumentRepository
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
import no.nav.aap.verdityper.dokument.Kanal
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
        register<FakePdfgenGateway>()
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

        InMemoryMottattDokumentRepository.lagre(
            mottattMeldekortDokument(
                meldekort,
                sak.id,
                behandling.id,
                begrunnelse = "Korrigering av timer",
                opprettetAv = "Z123456",
                opprettetTid = LocalDateTime.of(2025, 1, 20, 10, 0),
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
            val body = response.body<MeldeperioderMedMeldekortResponse>()
            assertThat(body.meldeperioderMedMeldekort).hasSize(1)

            val meldeperiodeMedMeldekort = body.meldeperioderMedMeldekort.first()
            assertThat(meldeperiodeMedMeldekort.meldeperiode).isEqualTo(meldeperiode)
            assertThat(meldeperiodeMedMeldekort.meldekort).isNotNull
            assertThat(meldeperiodeMedMeldekort.meldekort!!.id).isEqualTo(meldekort.journalpostId.identifikator)
            assertThat(meldeperiodeMedMeldekort.meldekort.meldeDato).isEqualTo(meldekort.mottattTidspunkt.toLocalDate())
            assertThat(meldeperiodeMedMeldekort.meldekort.oppdatertTidspunkt).isEqualTo(LocalDate.of(2025, 1, 20))
            assertThat(meldeperiodeMedMeldekort.meldekort.begrunnelse).isEqualTo("Korrigering av timer")
            assertThat(meldeperiodeMedMeldekort.meldekort.oppdatertAv).isEqualTo("Z123456")
            assertThat(meldeperiodeMedMeldekort.meldekort.dager).hasSize(2)
            assertThat(meldeperiodeMedMeldekort.meldekort.dager.map { it.dato }).containsExactlyInAnyOrder(dag1, dag2)
            assertThat(meldeperiodeMedMeldekort.meldekort.dager.map { it.timerArbeidet }).containsExactlyInAnyOrder(7.5, 3.0)
            assertThat(meldeperiodeMedMeldekort.tidligereMeldekort).isEmpty()
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

        InMemoryMottattDokumentRepository.lagre(mottattMeldekortDokument(meldekort1, sak.id, behandling.id))
        InMemoryMottattDokumentRepository.lagre(mottattMeldekortDokument(meldekort2, sak.id, behandling.id))

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

        InMemoryMottattDokumentRepository.lagre(
            mottattMeldekortDokument(
                opprinneligMeldekort,
                sak.id,
                behandling.id,
                opprettetTid = LocalDateTime.of(2025, 1, 20, 10, 0),
            )
        )
        InMemoryMottattDokumentRepository.lagre(
            mottattMeldekortDokument(
                korrigertMeldekort,
                sak.id,
                behandling.id,
                begrunnelse = "Feil i opprinnelig rapportering",
                opprettetAv = "Z654321",
                opprettetTid = LocalDateTime.of(2025, 1, 25, 11, 0),
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
            val body = response.body<MeldeperioderMedMeldekortResponse>()
            assertThat(body.meldeperioderMedMeldekort).hasSize(1)

            val meldeperiodeMedMeldekort = body.meldeperioderMedMeldekort.first()
            assertThat(meldeperiodeMedMeldekort.meldekort).isNotNull
            assertThat(meldeperiodeMedMeldekort.meldekort!!.id).isEqualTo(korrigertMeldekort.journalpostId.identifikator)
            assertThat(meldeperiodeMedMeldekort.meldekort.meldeDato).isEqualTo(korrigertMeldekort.mottattTidspunkt.toLocalDate())
            assertThat(meldeperiodeMedMeldekort.meldekort.oppdatertTidspunkt).isEqualTo(LocalDate.of(2025, 1, 25))
            assertThat(meldeperiodeMedMeldekort.meldekort.begrunnelse).isEqualTo("Feil i opprinnelig rapportering")
            assertThat(meldeperiodeMedMeldekort.meldekort.oppdatertAv).isEqualTo("Z654321")
            assertThat(meldeperiodeMedMeldekort.meldekort.dager).hasSize(1)
            assertThat(meldeperiodeMedMeldekort.meldekort.dager.first().timerArbeidet).isEqualTo(0.0)
            assertThat(meldeperiodeMedMeldekort.tidligereMeldekort).hasSize(1)
            assertThat(meldeperiodeMedMeldekort.tidligereMeldekort.first().id).isEqualTo(opprinneligMeldekort.journalpostId.identifikator)
            assertThat(meldeperiodeMedMeldekort.tidligereMeldekort.first().meldeDato).isEqualTo(opprinneligMeldekort.mottattTidspunkt.toLocalDate())
            assertThat(meldeperiodeMedMeldekort.tidligereMeldekort.first().oppdatertTidspunkt).isEqualTo(LocalDate.of(2025, 1, 20))
        }
    }

    @Test
    fun `returnerer tidligere meldekort sortert synkende på mottattTidspunkt`() {
        val sak = nySak()
        val behandling = opprettBehandling(sak, TypeBehandling.Førstegangsbehandling)

        InMemoryVedtakRepository.lagre(behandling.id, LocalDateTime.now(), LocalDate.now())

        val dag1 = 6 januar 2025
        val meldeperiode = Periode(dag1, dag1.plusDays(13))

        val førsteMeldekort = Meldekort(
            journalpostId = JournalpostId("111"),
            timerArbeidPerPeriode = setOf(
                ArbeidIPeriode(Periode(dag1, dag1), TimerArbeid(BigDecimal("7.5"))),
            ),
            mottattTidspunkt = LocalDateTime.of(2025, 1, 20, 9, 0)
        )
        val andreMeldekort = Meldekort(
            journalpostId = JournalpostId("222"),
            timerArbeidPerPeriode = setOf(
                ArbeidIPeriode(Periode(dag1, dag1), TimerArbeid(BigDecimal("5.0"))),
            ),
            mottattTidspunkt = LocalDateTime.of(2025, 1, 22, 9, 0)
        )
        val tredjeMeldekort = Meldekort(
            journalpostId = JournalpostId("333"),
            timerArbeidPerPeriode = setOf(
                ArbeidIPeriode(Periode(dag1, dag1), TimerArbeid(BigDecimal("3.0"))),
            ),
            mottattTidspunkt = LocalDateTime.of(2025, 1, 25, 9, 0)
        )

        InMemoryUnderveisRepository.lagre(
            behandlingId = behandling.id,
            underveisperioder = listOf(underveisperiode(Utfall.OPPFYLT, meldeperiode)),
            input = object : Faktagrunnlag {}
        )

        InMemoryMeldekortRepository.lagre(
            behandling.id, setOf(førsteMeldekort, andreMeldekort, tredjeMeldekort)
        )

        InMemoryMottattDokumentRepository.lagre(mottattMeldekortDokument(førsteMeldekort, sak.id, behandling.id))
        InMemoryMottattDokumentRepository.lagre(mottattMeldekortDokument(andreMeldekort, sak.id, behandling.id))
        InMemoryMottattDokumentRepository.lagre(mottattMeldekortDokument(tredjeMeldekort, sak.id, behandling.id))

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
            assertThat(meldeperiodeMedMeldekort.meldekort!!.id).isEqualTo(tredjeMeldekort.journalpostId.identifikator)

            assertThat(meldeperiodeMedMeldekort.tidligereMeldekort).hasSize(2)
            assertThat(meldeperiodeMedMeldekort.tidligereMeldekort.map { it.id })
                .containsExactly(
                    andreMeldekort.journalpostId.identifikator,
                    førsteMeldekort.journalpostId.identifikator
                )
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
    fun `returnerer kun oppfylte perioder når meldeperiode har delvis oppfylt utfall`() {
        val sak = nySak()
        val behandling = opprettBehandling(sak, TypeBehandling.Førstegangsbehandling)

        InMemoryVedtakRepository.lagre(behandling.id, LocalDateTime.now(), LocalDate.now())

        // Meldeperiode 1: ikke oppfylt (4 dager) + oppfylt standard sats (5 dager) + oppfylt annen sats (5 dager)
        val meldeperiode1 = Periode(6 januar 2025, 19 januar 2025)
        val mp1IkkeOppfylt = Periode(6 januar 2025, 9 januar 2025)
        val mp1OppfyltStandardSats = Periode(10 januar 2025, 14 januar 2025)
        val mp1OppfyltAnnenSats = Periode(15 januar 2025, 19 januar 2025)

        // Meldeperiode 2: oppfylt (8 dager) + ikke oppfylt (6 dager)
        val meldeperiode2 = Periode(20 januar 2025, 2 februar 2025)
        val mp2Oppfylt = Periode(20 januar 2025, 27 januar 2025)
        val mp2IkkeOppfylt = Periode(28 januar 2025, 2 februar 2025)

        InMemoryUnderveisRepository.lagre(
            behandlingId = behandling.id,
            underveisperioder = listOf(
                underveisperiode(Utfall.IKKE_OPPFYLT, mp1IkkeOppfylt, meldePeriode = meldeperiode1),
                underveisperiode(Utfall.OPPFYLT, mp1OppfyltStandardSats, meldePeriode = meldeperiode1),
                underveisperiode(Utfall.OPPFYLT, mp1OppfyltAnnenSats, meldePeriode = meldeperiode1, trekk = Dagsatser(2)),
                underveisperiode(Utfall.OPPFYLT, mp2Oppfylt, meldePeriode = meldeperiode2),
                underveisperiode(Utfall.IKKE_OPPFYLT, mp2IkkeOppfylt, meldePeriode = meldeperiode2),
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

            val sortert = body.meldeperioderMedMeldekort.sortedBy { it.meldeperiode.fom }

            // Meldeperiode 1: de to oppfylte periodene slås sammen til én sammenhengende periode
            val mp1 = sortert[0]
            assertThat(mp1.meldeperiode).isEqualTo(meldeperiode1)
            assertThat(mp1.perioder).containsExactly(Periode(10 januar 2025, 19 januar 2025))
            assertThat(mp1.meldekort).isNull()

            // Meldeperiode 2: kun den oppfylte perioden returneres
            val mp2 = sortert[1]
            assertThat(mp2.meldeperiode).isEqualTo(meldeperiode2)
            assertThat(mp2.perioder).containsExactly(mp2Oppfylt)
            assertThat(mp2.meldekort).isNull()
        }
    }

    @Test
    fun `prosessering - returnerer KLAR når ingen ventende meldekort-jobber`() {
        val sak = nySak()
        opprettBehandling(sak, TypeBehandling.Førstegangsbehandling)

        testApplication {
            installApplication {
                meldekortApi(MockDataSource(), inMemoryRepositoryRegistry, createTestGatewayProvider(), fixedClock)
            }

            val response = createClient().get("/api/meldekort/${sak.saksnummer}/prosessering") {
                header("Authorization", "Bearer ${getToken().token()}")
            }

            assertThat(response.status).isEqualTo(HttpStatusCode.OK)
            val body = response.body<MeldekortProsesseringResponse>()
            assertThat(body.meldekortProsesseringStatus).isEqualTo(MeldekortProsesseringStatus.KLAR)
        }
    }

    @Test
    fun `prosessering - returnerer PROSESSERER_MELDEKORT når det finnes ventende meldekort-jobber`() {
        val sak = nySak()
        opprettBehandling(sak, TypeBehandling.Førstegangsbehandling)

        InMemoryFlytJobbRepository.leggTil(
            HendelseMottattHåndteringJobbUtfører.nyJobb(
                sakId = sak.id,
                dokumentReferanse = InnsendingReferanse(JournalpostId("999")),
                brevkategori = InnsendingType.MELDEKORT,
                kanal = Kanal.DIGITAL,
                mottattTidspunkt = LocalDateTime.of(2025, 1, 20, 9, 0),
            )
        )

        testApplication {
            installApplication {
                meldekortApi(MockDataSource(), inMemoryRepositoryRegistry, createTestGatewayProvider(), fixedClock)
            }

            val response = createClient().get("/api/meldekort/${sak.saksnummer}/prosessering") {
                header("Authorization", "Bearer ${getToken().token()}")
            }

            assertThat(response.status).isEqualTo(HttpStatusCode.OK)
            val body = response.body<MeldekortProsesseringResponse>()
            assertThat(body.meldekortProsesseringStatus).isEqualTo(MeldekortProsesseringStatus.PROSESSERER_MELDEKORT)
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
            meldeperiode = Periode(dag1, dag2),
            begrunnelse = "Korrigering av timer",
            meldeDato = dag1,
            dager = setOf(
                DagDto(dato = dag1, timerArbeidet = 7.5),
                DagDto(dato = dag2, timerArbeidet = 3.0),
            ),
        )

        testApplication {
            installApplication {
                meldekortApi(MockDataSource(), inMemoryRepositoryRegistry, createTestGatewayProviderMedDokarkiv(), fixedClock)
            }

            val response = createClient().post("/api/meldekort/${sak.saksnummer}") {
                header("Authorization", "Bearer ${getToken().token()}")
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            assertThat(response.status).isEqualTo(HttpStatusCode.OK)
            val body = response.body<OppdaterMeldekortResponse>()
            assertThat(body.journalpostId).isNotBlank()
            // fixedClock = 2025-04-01T00:00:00Z = 2025-04-01T02:00:00 i Europe/Oslo (CEST)
            assertThat(body.oppdatertTidspunkt).isEqualTo(LocalDate.of(2025, 4, 1))
        }
    }

    private fun underveisperiode(utfall: Utfall, periode: Periode, meldePeriode: Periode = periode, trekk: Dagsatser = Dagsatser(0)) = Underveisperiode(
        periode = periode,
        meldePeriode = meldePeriode,
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
        trekk = trekk,
        brukerAvKvoter = emptySet(),
        institusjonsoppholdReduksjon = `0_PROSENT`,
        meldepliktStatus = MeldepliktStatus.MELDT_SEG,
        meldepliktGradering = `0_PROSENT`,
    )

    private fun mottattMeldekortDokument(
        meldekort: Meldekort,
        sakId: SakId,
        behandlingId: BehandlingId,
        begrunnelse: String? = null,
        opprettetAv: String? = null,
        opprettetTid: LocalDateTime = LocalDateTime.now(),
    ) =
        MottattDokument(
            referanse = InnsendingReferanse(meldekort.journalpostId),
            sakId = sakId,
            behandlingId = behandlingId,
            mottattTidspunkt = meldekort.mottattTidspunkt,
            opprettetTid = opprettetTid,
            type = InnsendingType.MELDEKORT,
            kanal = Kanal.DIGITAL,
            strukturertDokument = StrukturertDokument(
                MeldekortV0(
                    harDuArbeidet = meldekort.timerArbeidPerPeriode.any { it.timerArbeid.antallTimer > BigDecimal.ZERO },
                    timerArbeidPerPeriode = meldekort.timerArbeidPerPeriode.map { arbeid ->
                        ArbeidIPeriodeV0(
                            fraOgMedDato = arbeid.periode.fom,
                            tilOgMedDato = arbeid.periode.tom,
                            timerArbeid = arbeid.timerArbeid.antallTimer.toDouble(),
                        )
                    },
                    begrunnelse = begrunnelse,
                    opprettetAv = opprettetAv,
                )
            ),
        )
}
