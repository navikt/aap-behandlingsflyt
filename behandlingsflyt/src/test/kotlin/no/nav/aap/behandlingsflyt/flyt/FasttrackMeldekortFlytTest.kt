package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarBistandsbehovLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.SykdomsvurderingForBrevLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.Underveisperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.StrukturertDokument
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.MeldekortGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.MeldekortRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.flate.BistandVurderingLøsningDto
import no.nav.aap.behandlingsflyt.flyt.internals.DokumentMottattPersonHendelse
import no.nav.aap.behandlingsflyt.help.assertTidslinje
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.ArbeidIPeriodeV0
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.MeldekortV0
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.repository.avklaringsbehov.AvklaringsbehovRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.behandling.tilkjentytelse.TilkjentYtelseRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.meldeperiode.MeldeperiodeRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.underveis.UnderveisRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.postgresRepositoryRegistry
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.test.FakeUnleashFasttrackMeldekort
import no.nav.aap.behandlingsflyt.test.august
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.komponenter.verdityper.Prosent.Companion.`0_PROSENT`
import no.nav.aap.komponenter.verdityper.Prosent.Companion.`50_PROSENT`
import no.nav.aap.verdityper.dokument.JournalpostId
import org.assertj.core.api.Assertions.assertThat
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.reflect.KClass
import kotlin.test.Test

class FasttrackMeldekortFlytTest :
    AbstraktFlytOrkestratorTest(FakeUnleashFasttrackMeldekort::class as KClass<UnleashGateway>) {

    @Test
    fun `Meldekortgrunnlag skal flettes inn i åpen behandling før UnderveisSteg`() {
        val sak = happyCaseFørstegangsbehandling()
        val åpenBehandling = revurdereFramTilOgMedSykdom(sak, sak.rettighetsperiode.fom)

        assertThat(åpenBehandling.vurderingsbehov().map { it.type })
            .hasSameElementsAs(listOf(Vurderingsbehov.MOTTATT_SØKNAD))
        val aktivtStegFørMeldekort = åpenBehandling.aktivtSteg()

        åpenBehandling.sendInnMeldekort(sak.rettighetsperiode)

        dataSource.transaction { connection ->
            val behandlingRepo = BehandlingRepositoryImpl(connection)
            val behandlinger = behandlingRepo.hentAlleFor(åpenBehandling.sakId)
            assertThat(behandlinger).hasSize(3)
            val meldekortbehandling = behandlinger.maxBy { it.opprettetTidspunkt }
            assertThat(meldekortbehandling.årsakTilOpprettelse)
                .isEqualTo(ÅrsakTilOpprettelse.MELDEKORT)
            assertThat(meldekortbehandling.vurderingsbehov().map { it.type })
                .hasSameElementsAs(listOf(Vurderingsbehov.MOTTATT_MELDEKORT))
            assertThat(meldekortbehandling.status()).isEqualTo(Status.AVSLUTTET)

            val åpenBehandling = behandlinger
                .filter { it.typeBehandling() == TypeBehandling.Revurdering }
                .minBy { it.opprettetTidspunkt }
            // Åpen behandling skal forbli i samme steg
            assertThat(åpenBehandling.aktivtSteg())
                .describedAs("Meldekortbehandling skal ikke endre steg for åpen behandling, dersom den åpne behandlingen står i steg før informasjonskravet for meldekort")
                .isEqualTo(aktivtStegFørMeldekort)

            // Den åpne behandlingen skal ha fått meldekortgrunnlaget
            val meldekortGrunnlag = hentMeldekortGrunnlag(connection, åpenBehandling.id)
            assertThat(meldekortGrunnlag).isNotNull
            assertThat(meldekortGrunnlag!!.meldekortene).hasSize(2)
        }
    }

    @Test
    fun `Åpen behandling skal tilbakeføres til UnderveisSteg etter fullført meldekortbehandling`() {
        val sak = happyCaseFørstegangsbehandling()

        val revurderingGjelderFra = sak.rettighetsperiode.fom.plusWeeks(2)
        var åpenBehandling = revurdereFramTilOgMedSykdom(sak, revurderingGjelderFra)

        åpenBehandling = åpenBehandling.løsAvklaringsBehov(
            AvklarBistandsbehovLøsning(
                bistandsVurdering = BistandVurderingLøsningDto(
                    begrunnelse = "Trenger hjelp fra nav",
                    erBehovForAktivBehandling = true,
                    erBehovForArbeidsrettetTiltak = false,
                    erBehovForAnnenOppfølging = null,
                    skalVurdereAapIOvergangTilUføre = null,
                    skalVurdereAapIOvergangTilArbeid = null,
                    overgangBegrunnelse = null
                ),
            )
        )
            .medKontekst {
                assertThat(this.åpneAvklaringsbehov).extracting<Definisjon> { it.definisjon }
                    .containsExactlyInAnyOrder(Definisjon.SKRIV_SYKDOMSVURDERING_BREV)
            }
            .løsAvklaringsBehov(
                SykdomsvurderingForBrevLøsning(
                    vurdering = "Begrunnelse"
                ),
            )
            .medKontekst {
                assertThat(this.åpneAvklaringsbehov).extracting<Definisjon> { it.definisjon }
                    .containsExactlyInAnyOrder(Definisjon.FATTE_VEDTAK)
            }

        assertThat(åpenBehandling.vurderingsbehov().map { it.type })
            .hasSameElementsAs(listOf(Vurderingsbehov.MOTTATT_SØKNAD))

        val aktivtStegFørMeldekort = åpenBehandling.aktivtSteg()
        assertThat(aktivtStegFørMeldekort).isEqualTo(StegType.FATTE_VEDTAK)

        val underveisGrunnlag = dataSource.transaction { connection ->
            UnderveisRepositoryImpl(connection).hent(åpenBehandling.id)
        }
        assertThat(underveisGrunnlag.perioder).isNotEmpty
        assertThat(underveisGrunnlag.perioder).extracting<RettighetsType>(Underveisperiode::rettighetsType)
            .allSatisfy { rettighetsType ->
                assertThat(rettighetsType).isEqualTo(RettighetsType.BISTANDSBEHOV)
            }
        val tilkjentYtelseFørMeldekort = dataSource.transaction {
            TilkjentYtelseRepositoryImpl(it).hentHvisEksisterer(åpenBehandling.id)
        }

        åpenBehandling.sendInnMeldekort(
            sak.rettighetsperiode, listOf(
                ArbeidIPeriodeV0(
                    fraOgMedDato = revurderingGjelderFra,
                    tilOgMedDato = revurderingGjelderFra.plusDays(13),
                    timerArbeid = 5.0,
                )
            )
        )

        åpenBehandling = dataSource.transaction { connection ->

            val behandlinger = BehandlingRepositoryImpl(connection).hentAlleFor(åpenBehandling.sakId)

            assertThat(behandlinger).hasSize(3)
            val meldekortbehandling = behandlinger.maxBy { it.opprettetTidspunkt }
            assertThat(meldekortbehandling.vurderingsbehov().map { it.type })
                .hasSameElementsAs(listOf(Vurderingsbehov.MOTTATT_MELDEKORT))
            assertThat(meldekortbehandling.årsakTilOpprettelse)
                .isEqualTo(ÅrsakTilOpprettelse.MELDEKORT)

            AvklaringsbehovRepositoryImpl(connection)
                .hentAvklaringsbehovene(meldekortbehandling.id)
                .alle()
                .filter { it.erÅpent() }
                .also { avklaringsbehov ->
                    assertThat(avklaringsbehov).isEmpty()
                }

            BehandlingRepositoryImpl(connection).hentStegHistorikk(meldekortbehandling.id).also { steghistorikk ->
                assertThat(steghistorikk).isNotEmpty
            }

            assertThat(meldekortbehandling.status()).isEqualTo(Status.AVSLUTTET)
            TilkjentYtelseRepositoryImpl(connection).hentHvisEksisterer(meldekortbehandling.id)
                .also { tilkjentYtelse ->
                    assertThat(tilkjentYtelse).isNotNull
                }

            val åpenBehandling = behandlinger
                .filter { it.typeBehandling() == TypeBehandling.Revurdering }
                .minBy { it.opprettetTidspunkt }
            assertThat(åpenBehandling.aktivtSteg()).isEqualTo(StegType.FATTE_VEDTAK)

            åpenBehandling
        }

        prosesserBehandling(åpenBehandling)
        val underveisGrunnlag2 = dataSource.transaction { connection ->
            UnderveisRepositoryImpl(connection).hent(åpenBehandling.id)
        }

        assertThat(underveisGrunnlag).isNotEqualTo(underveisGrunnlag2)

        val tilkjentYtelseEtterMeldekort = dataSource.transaction {
            TilkjentYtelseRepositoryImpl(it).hentHvisEksisterer(åpenBehandling.id)
        }
        assertThat(tilkjentYtelseFørMeldekort).isNotEqualTo(tilkjentYtelseEtterMeldekort)
    }

    @Test
    fun `sender inn to meldekort, resultat reflektert i åpen behandling`() {
        val sak = happyCaseFørstegangsbehandling(fom = 25 august 2025)
        val åpenBehandling = revurdereFramTilOgMedSykdom(sak, sak.rettighetsperiode.fom.plusWeeks(2))
        åpenBehandling.løsSykdom().løsBistand().løsSykdomsvurderingBrev()

        val (førsteMeldeperiode, andreMeldeperiode) = dataSource.transaction { connection ->
            MeldeperiodeRepositoryImpl(connection).hent(åpenBehandling.id)
        }
        åpenBehandling.sendInnMeldekort(
            sak.rettighetsperiode,
            listOf(
                ArbeidIPeriodeV0(
                    fraOgMedDato = førsteMeldeperiode.fom,
                    tilOgMedDato = førsteMeldeperiode.tom,
                    timerArbeid = 37.5 / 2,
                ),
            ),
            mottattTidspunkt = førsteMeldeperiode.tom.plusDays(1).atTime(8, 0),
        )
        åpenBehandling.sendInnMeldekort(
            sak.rettighetsperiode, listOf(
                ArbeidIPeriodeV0(
                    fraOgMedDato = andreMeldeperiode.fom,
                    tilOgMedDato = andreMeldeperiode.tom,
                    timerArbeid = 37.5,
                )
            ),
            mottattTidspunkt = andreMeldeperiode.tom.plusDays(1).atTime(8, 0),
        )
        motor.kjørJobber()

        dataSource.transaction { connection ->
            val behandlinger = BehandlingRepositoryImpl(connection).hentAlleFor(åpenBehandling.sakId)
                .let { it.sortedWith(BehandlingCompare(it)) }

            assertThat(behandlinger).hasSize(4)
            val (førstegangsbehandling, førsteMeldekort, andreMeldekort, åpenBehandling) = behandlinger

            assertThat(førstegangsbehandling.årsakTilOpprettelse)
                .isEqualTo(ÅrsakTilOpprettelse.SØKNAD)
            assertTidslinje(
                andelArbeidetTidslinje(connection, førstegangsbehandling),
                sak.rettighetsperiode to {
                    assertThat(it).isEqualTo(`0_PROSENT`)
                },
            )

            assertThat(førsteMeldekort.årsakTilOpprettelse)
                .isEqualTo(ÅrsakTilOpprettelse.MELDEKORT)
            assertTidslinje(
                andelArbeidetTidslinje(connection, førsteMeldekort),
                førsteMeldeperiode to {
                    assertThat(it).isEqualTo(Prosent(24))
                },
                Periode(førsteMeldeperiode.tom.plusDays(1), sak.rettighetsperiode.tom) to {
                    assertThat(it).isEqualTo(`0_PROSENT`)
                },
            )

            assertThat(andreMeldekort.årsakTilOpprettelse)
                .isEqualTo(ÅrsakTilOpprettelse.MELDEKORT)
            assertTidslinje(
                andelArbeidetTidslinje(connection, andreMeldekort),
                førsteMeldeperiode to {
                    assertThat(it).isEqualTo(Prosent(24))
                },
                andreMeldeperiode to {
                    assertThat(it).isEqualTo(`50_PROSENT`)
                },
                Periode(andreMeldeperiode.tom.plusDays(1), sak.rettighetsperiode.tom) to {
                    assertThat(it).isEqualTo(`0_PROSENT`)
                },
            )

            assertThat(åpenBehandling.årsakTilOpprettelse)
                .isEqualTo(ÅrsakTilOpprettelse.SØKNAD)
            assertTidslinje(
                andelArbeidetTidslinje(connection, åpenBehandling),
                førsteMeldeperiode to {
                    assertThat(it).isEqualTo(Prosent(24))
                },
                andreMeldeperiode to {
                    assertThat(it).isEqualTo(`50_PROSENT`)
                },
                Periode(andreMeldeperiode.tom.plusDays(1), sak.rettighetsperiode.tom) to {
                    assertThat(it).isEqualTo(`0_PROSENT`)
                },
            )
        }
    }

    private fun andelArbeidetTidslinje(connection: DBConnection, behandling: Behandling): Tidslinje<Prosent> {
        return UnderveisRepositoryImpl(connection).hent(behandling.id)
            .perioder
            .map { Segment(it.periode, it.arbeidsgradering.andelArbeid) }
            .let { Tidslinje(it) }
    }

    private class BehandlingCompare(behandlinger: List<Behandling>): Comparator<Behandling> {
        private val behandlinger = behandlinger.associateBy { it.id }

        override fun compare(o1: Behandling, o2: Behandling): Int {
            if (o1.id == o2.id) {
                return 0
            }

            var current: Behandling? = o1
            while (current != null) {
                if (current.id == o2.id) {
                    return 1
                }
                current = behandlinger[current.forrigeBehandlingId]
            }
            return -1
        }
    }

    fun hentMeldekortGrunnlag(connection: DBConnection, behandlingId: BehandlingId): MeldekortGrunnlag? {
        val repositoryProvider = postgresRepositoryRegistry.provider(connection)
        val meldekortRespoitory: MeldekortRepository = repositoryProvider.provide()
        return meldekortRespoitory.hentHvisEksisterer(behandlingId)
    }

    private var nesteJournalpostId = (300..1000000)
        .asSequence()
        .map { JournalpostId(it.toString()) }
        .iterator()

    private fun Behandling.sendInnMeldekort(
        periode: Periode,
        timerArbeidIPeriode: List<ArbeidIPeriodeV0> = listOf(
            ArbeidIPeriodeV0(
                fraOgMedDato = LocalDate.now().minusMonths(3),
                tilOgMedDato = LocalDate.now().plusMonths(3),
                timerArbeid = 0.0,
            )
        ),
        journalpostId: JournalpostId = nesteJournalpostId.next(),
        mottattTidspunkt: LocalDateTime = LocalDateTime.now()
    ) {
        this.sendInnDokument(
            DokumentMottattPersonHendelse(
                journalpost = journalpostId,
                mottattTidspunkt = mottattTidspunkt,
                strukturertDokument = StrukturertDokument(
                    MeldekortV0(
                        harDuArbeidet = timerArbeidIPeriode.any { it.timerArbeid > 0.0 },
                        timerArbeidPerPeriode = timerArbeidIPeriode
                    ),
                ),
                periode = periode
            )
        )
    }
}