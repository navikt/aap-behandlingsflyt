package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.SykdomsvurderingForBrevLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.Underveisperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.MeldekortGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.MeldekortRepository
import no.nav.aap.behandlingsflyt.help.assertTidslinje
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.repository.avklaringsbehov.AvklaringsbehovRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.behandling.tilkjentytelse.TilkjentYtelseRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.meldeperiode.MeldeperiodeRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.underveis.UnderveisRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.postgresRepositoryRegistry
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.test.FakeUnleash
import no.nav.aap.behandlingsflyt.test.august
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.komponenter.verdityper.Prosent.Companion.`0_PROSENT`
import org.assertj.core.api.Assertions.assertThat
import java.time.LocalDate
import kotlin.test.Test

class FasttrackMeldekortFlytTest :
    AbstraktFlytOrkestratorTest(FakeUnleash::class) {

    @Test
    fun `Meldekortgrunnlag skal flettes inn i åpen behandling før UnderveisSteg`() {
        val søknadsdato = LocalDate.now().minusMonths(3)
        val sak = happyCaseFørstegangsbehandling(søknadsdato)
        val åpenBehandling = revurdereFramTilOgMedSykdom(sak, sak.rettighetsperiode.fom)

        val aktivtStegFørMeldekort = åpenBehandling.aktivtSteg()

        sak.sendInnMeldekort(
            journalpostId = journalpostId(),
            timerArbeidet = mapOf(søknadsdato to 5.0),
        )

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

        åpenBehandling = åpenBehandling.løsBistand()
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

        sak.sendInnMeldekort(
            journalpostId = journalpostId(),
            timerArbeidet = Periode(revurderingGjelderFra, revurderingGjelderFra.plusDays(13))
                .dager()
                .associateWith { 1.0 }
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
        sak.sendInnMeldekort(
            journalpostId = journalpostId(),
            timerArbeidet = førsteMeldeperiode.dager().associateWith { 1.0 },
            mottattTidspunkt = førsteMeldeperiode.tom.plusDays(1).atTime(8, 0),
        )
        sak.sendInnMeldekort(
            journalpostId = journalpostId(),
            timerArbeidet = andreMeldeperiode.dager().associateWith { 2.0 },
            mottattTidspunkt = andreMeldeperiode.tom.plusDays(1).atTime(8, 0),
        )
        motor.kjørJobber()

        dataSource.transaction { connection ->
            val behandlinger = BehandlingRepositoryImpl(connection).hentAlleFor(åpenBehandling.sakId)
                .let { it.sortedWith(BehandlingCompare(it)) }

            assertThat(behandlinger).hasSize(4)
            val (førstegangsbehandling, førsteMeldekort, andreMeldekort, åpenBehandling) = behandlinger

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
                    assertThat(it).isEqualTo(Prosent(18))
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
                    assertThat(it).isEqualTo(Prosent(18))
                },
                andreMeldeperiode to {
                    assertThat(it).isEqualTo(Prosent(37))
                },
                Periode(andreMeldeperiode.tom.plusDays(1), sak.rettighetsperiode.tom) to {
                    assertThat(it).isEqualTo(`0_PROSENT`)
                },
            )

            assertTidslinje(
                andelArbeidetTidslinje(connection, åpenBehandling),
                førsteMeldeperiode to {
                    assertThat(it).isEqualTo(Prosent(18))
                },
                andreMeldeperiode to {
                    assertThat(it).isEqualTo(Prosent(37))
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

}