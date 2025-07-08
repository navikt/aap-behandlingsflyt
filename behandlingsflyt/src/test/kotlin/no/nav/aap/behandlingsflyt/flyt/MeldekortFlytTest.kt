package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarBistandsbehovLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarSykepengerErstatningLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.ForeslåVedtakLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.RefusjonkravLøsning
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.TilkjentYtelseService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.Underveisperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.StrukturertDokument
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.MeldekortGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.MeldekortRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.MeldekortService
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.flate.BistandVurderingLøsningDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.refusjonkrav.RefusjonkravVurderingDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykepengerGrunn
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.flate.SykepengerVurderingDto
import no.nav.aap.behandlingsflyt.flyt.internals.DokumentMottattPersonHendelse
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.ArbeidIPeriodeV0
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.MeldekortV0
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.SøknadMedlemskapDto
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.SøknadStudentDto
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.SøknadV0
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.repository.avklaringsbehov.AvklaringsbehovRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.behandling.tilkjentytelse.TilkjentYtelseRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.underveis.UnderveisRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.postgresRepositoryRegistry
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.ÅrsakTilBehandling
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.verdityper.dokument.JournalpostId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class MeldekortFlytTest : AbstraktFlytOrkestratorTest() {

    @Test
    fun `Meldekortgrunnlag skal flettes inn i åpen behandling før UnderveisSteg`() {
        val sak = happyCaseFørstegansbehandling()
        val åpenBehandling = revurdereFramTilOgMedSykdom(sak, sak.rettighetsperiode.fom)

        assertThat(
            åpenBehandling.årsaker().map { it.type }).hasSameElementsAs(listOf(ÅrsakTilBehandling.MOTTATT_SØKNAD))
        val aktivtStegFørMeldekort = åpenBehandling.aktivtSteg()

        åpenBehandling.sendInnMeldekort(sak.rettighetsperiode)

        dataSource.transaction { connection ->
            val behandlingRepo = BehandlingRepositoryImpl(connection)
            val behandlinger = behandlingRepo.hentAlleFor(åpenBehandling.sakId)
            assertThat(behandlinger).hasSize(3)
            val meldekortbehandling = behandlinger.maxBy { it.opprettetTidspunkt }
            assertThat(
                meldekortbehandling.årsaker()
                    .map { it.type }).hasSameElementsAs(
                listOf(
                    ÅrsakTilBehandling.MOTTATT_MELDEKORT,
                )
            )
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
        val sak = happyCaseFørstegansbehandling()

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
                    .containsExactlyInAnyOrder(Definisjon.FORESLÅ_VEDTAK)
            }
            .løsAvklaringsBehov(ForeslåVedtakLøsning())


        assertThat(
            åpenBehandling.årsaker().map { it.type }).hasSameElementsAs(listOf(ÅrsakTilBehandling.MOTTATT_SØKNAD))

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
            assertThat(
                meldekortbehandling.årsaker()
                    .map { it.type }).hasSameElementsAs(
                listOf(
                    ÅrsakTilBehandling.MOTTATT_MELDEKORT,
                )
            )
            val avklaringsbehov = AvklaringsbehovRepositoryImpl(connection).hentAvklaringsbehovene(meldekortbehandling.id).alle()
                .filter { it.erÅpent() }
            assertThat(avklaringsbehov).isEmpty()
            val steghistorikk  = BehandlingRepositoryImpl(connection).hentStegHistorikk(meldekortbehandling.id)
            assertThat(steghistorikk).isNotEmpty

            assertThat(meldekortbehandling.status()).isEqualTo(Status.AVSLUTTET)
            val tilkjentYtelse = TilkjentYtelseRepositoryImpl(connection).hentHvisEksisterer(meldekortbehandling.id)
            assertThat(tilkjentYtelse).isNotNull

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

    }

    fun hentMeldekortGrunnlag(connection: DBConnection, behandlingId: BehandlingId): MeldekortGrunnlag? {
        val repositoryProvider = postgresRepositoryRegistry.provider(connection)
        val meldekortRespoitory: MeldekortRepository = repositoryProvider.provide()
        return meldekortRespoitory.hentHvisEksisterer(behandlingId)
    }

    private fun Behandling.sendInnMeldekort(
        periode: Periode, timerArbeidIPeriode: List<ArbeidIPeriodeV0> = listOf(
            ArbeidIPeriodeV0(
                fraOgMedDato = LocalDate.now().minusMonths(3),
                tilOgMedDato = LocalDate.now().plusMonths(3),
                timerArbeid = 0.0,
            )
        )
    ) {
        this.sendInnDokument(
            DokumentMottattPersonHendelse(
                journalpost = JournalpostId("300"),
                mottattTidspunkt = LocalDateTime.now(),
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