package no.nav.aap.behandlingsflyt.repository.behandling

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.BeregningsgrunnlagRepositoryImpl
import no.nav.aap.behandlingsflyt.help.FakePdlGateway
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.repository.behandling.brev.bestilling.BrevbestillingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.behandling.tilkjentytelse.TilkjentYtelseRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.behandling.vedtak.VedtakRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.barnetillegg.BarnetilleggRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.meldeperiode.MeldeperiodeRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.samordning.SamordningAndreStatligeYtelserRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.samordning.SamordningRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.samordning.SamordningUføreRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.samordning.SamordningYtelseRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.samordning.tjenestepensjon.TjenestePensjonRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.samordning.ytelsesvurdering.SamordningVurderingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.underveis.UnderveisRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.dokument.arbeid.MeldekortRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.klage.FormkravRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.medlemskaplovvalg.MedlemskapArbeidInntektForutgåendeRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.medlemskaplovvalg.MedlemskapArbeidInntektRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.personopplysning.PersonopplysningForutgåendeRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.personopplysning.PersonopplysningRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.register.barn.BarnRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.register.inntekt.InntektGrunnlagRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.register.inntekt.ManuellInntektGrunnlagRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.register.institusjonsopphold.InstitusjonsoppholdRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.register.uføre.UføreRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.register.yrkesskade.YrkesskadeRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.arbeidsevne.ArbeidsevneRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.beregning.BeregningVurderingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.bistand.BistandRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.meldeplikt.MeldepliktRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.refusjonkrav.RefusjonkravRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.rettighetsperiode.VurderRettighetsperiodeRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.student.StudentRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.sykdom.SykdomRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.sykdom.SykepengerErstatningRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.søknad.TrukketSøknadRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.log.ContextRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.lås.TaSkriveLåsRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.pip.PipRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.PersonRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.test.ident
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

internal class BehandlingRepositoryImplTest {
    companion object {
        private val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))

        private lateinit var dataSource: TestDataSource

        @BeforeAll
        @JvmStatic
        fun setup() {
            dataSource = TestDataSource()
        }

        @AfterAll
        @JvmStatic
        fun tearDown() = dataSource.close()
    }

    @Test
    fun `Kan lagre og hente ut behandling med uuid`() {
        val skapt = dataSource.transaction { connection ->
            val sak = PersonOgSakService(
                FakePdlGateway,
                PersonRepositoryImpl(connection),
                SakRepositoryImpl(connection)
            ).finnEllerOpprett(
                ident(),
                Periode(LocalDate.now(), LocalDate.now().plusYears(3))
            )
            val behandlingRepo = BehandlingRepositoryImpl(connection)

            // Opprett
            behandlingRepo.opprettBehandling(
                sakId = sak.id,
                typeBehandling = TypeBehandling.Førstegangsbehandling,
                forrigeBehandlingId = null,
                vurderingsbehovOgÅrsak = VurderingsbehovOgÅrsak(
                    vurderingsbehov = listOf(VurderingsbehovMedPeriode(type = Vurderingsbehov.MOTTATT_SØKNAD)),
                    årsak = ÅrsakTilOpprettelse.SØKNAD
                ),
            )
        }

        dataSource.transaction { connection ->
            val behandlingRepo = BehandlingRepositoryImpl(connection)

            // Hent ut igjen
            val hententMedReferanse = behandlingRepo.hent(skapt.referanse)

            assertThat(hententMedReferanse.referanse).isEqualTo(skapt.referanse)
            assertThat(hententMedReferanse.vurderingsbehov()).containsExactlyElementsOf(skapt.vurderingsbehov())
            assertThat(hententMedReferanse.vurderingsbehov()).containsExactlyElementsOf(
                listOf(
                    VurderingsbehovMedPeriode(
                        type = Vurderingsbehov.MOTTATT_SØKNAD
                    )
                )
            )
            assertThat(hententMedReferanse.typeBehandling()).isEqualTo(TypeBehandling.Førstegangsbehandling)
            assertThat(hententMedReferanse.årsakTilOpprettelse).isEqualTo(ÅrsakTilOpprettelse.SØKNAD)
        }
    }

    @Test
    fun `Opprettet dato lagres på behandling og hentes ut korrekt`() {
        val skapt = dataSource.transaction { connection ->
            val sak = PersonOgSakService(
                FakePdlGateway,
                PersonRepositoryImpl(connection),
                SakRepositoryImpl(connection)
            ).finnEllerOpprett(
                ident(),
                Periode(LocalDate.now(), LocalDate.now().plusYears(3))
            )
            val behandlingRepo = BehandlingRepositoryImpl(connection)

            // Opprett
            behandlingRepo.opprettBehandling(
                sakId = sak.id,
                typeBehandling = TypeBehandling.Førstegangsbehandling,
                forrigeBehandlingId = null,
                vurderingsbehovOgÅrsak = VurderingsbehovOgÅrsak(
                    vurderingsbehov = listOf(VurderingsbehovMedPeriode(type = Vurderingsbehov.MOTTATT_SØKNAD)),
                    årsak = ÅrsakTilOpprettelse.SØKNAD
                ),
            )
        }

        dataSource.transaction { connection ->
            val behandlingRepo = BehandlingRepositoryImpl(connection)

            // Hent ut igjen
            val hententMedReferanse = behandlingRepo.hent(skapt.referanse)

            assertThat(hententMedReferanse.opprettetTidspunkt).isEqualTo(skapt.opprettetTidspunkt)
        }
    }

    @Test
    fun `Kan hente ut behandlinger for sak filtrert på type`() {
        val (sak, førstegang, klage) = dataSource.transaction { connection ->
            val sak = PersonOgSakService(
                FakePdlGateway,
                PersonRepositoryImpl(connection),
                SakRepositoryImpl(connection)
            ).finnEllerOpprett(
                ident(),
                Periode(LocalDate.now(), LocalDate.now().plusYears(3))
            )
            val behandlingRepo = BehandlingRepositoryImpl(connection)

            // Opprett
            val førstegang = behandlingRepo.opprettBehandling(
                sakId = sak.id,
                typeBehandling = TypeBehandling.Førstegangsbehandling,
                forrigeBehandlingId = null,
                vurderingsbehovOgÅrsak = VurderingsbehovOgÅrsak(
                    vurderingsbehov = listOf(VurderingsbehovMedPeriode(type = Vurderingsbehov.MOTTATT_SØKNAD)),
                    årsak = ÅrsakTilOpprettelse.SØKNAD
                ),
            )

            val klage = behandlingRepo.opprettBehandling(
                sakId = sak.id,
                typeBehandling = TypeBehandling.Klage,
                forrigeBehandlingId = null,
                vurderingsbehovOgÅrsak = VurderingsbehovOgÅrsak(
                    vurderingsbehov = listOf(VurderingsbehovMedPeriode(type = Vurderingsbehov.MOTATT_KLAGE)),
                    årsak = ÅrsakTilOpprettelse.KLAGE
                ),
            )
            Triple(sak, førstegang, klage)
        }

        dataSource.transaction { connection ->
            val behandlingRepo = BehandlingRepositoryImpl(connection)

            // Hent ut igjen
            val alleDefault = behandlingRepo.hentAlleFor(sak.id)
            assertThat(alleDefault).hasSize(2)

            val alleFørstegang = behandlingRepo.hentAlleFor(sak.id, listOf(TypeBehandling.Førstegangsbehandling))
            assertThat(alleFørstegang).hasSize(1)
            assertThat(alleFørstegang[0].referanse).isEqualTo(førstegang.referanse)

            val alleKlage = behandlingRepo.hentAlleFor(sak.id, listOf(TypeBehandling.Klage))
            assertThat(alleKlage).hasSize(1)
            assertThat(alleKlage[0].referanse).isEqualTo(klage.referanse)
        }
    }

    @Test
    fun `Kan hente ut behandlinger med vedtak for person`() {
        val vedtakstidspunkt = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS)
        val virkningstidspunkt = LocalDate.now().plusMonths(1)


        val (sak, førstegang, klage) = dataSource.transaction { connection ->
            val sak = PersonOgSakService(
                FakePdlGateway,
                PersonRepositoryImpl(connection),
                SakRepositoryImpl(connection)
            ).finnEllerOpprett(
                ident(),
                Periode(LocalDate.now(), LocalDate.now().plusYears(3))
            )
            val behandlingRepo = BehandlingRepositoryImpl(connection)
            val vedtakRepo = VedtakRepositoryImpl(connection)

            // Opprett
            val førstegang = behandlingRepo.opprettBehandling(
                sakId = sak.id,
                typeBehandling = TypeBehandling.Førstegangsbehandling,
                forrigeBehandlingId = null,
                vurderingsbehovOgÅrsak = VurderingsbehovOgÅrsak(
                    vurderingsbehov = listOf(VurderingsbehovMedPeriode(type = Vurderingsbehov.MOTTATT_SØKNAD)),
                    årsak = ÅrsakTilOpprettelse.SØKNAD
                ),
            )

            vedtakRepo.lagre(
                behandlingId = førstegang.id,
                vedtakstidspunkt = vedtakstidspunkt,
                virkningstidspunkt = virkningstidspunkt,
            )

            val klage = behandlingRepo.opprettBehandling(
                sakId = sak.id,
                typeBehandling = TypeBehandling.Klage,
                forrigeBehandlingId = null,
                vurderingsbehovOgÅrsak = VurderingsbehovOgÅrsak(
                    vurderingsbehov = listOf(VurderingsbehovMedPeriode(type = Vurderingsbehov.MOTATT_KLAGE)),
                    årsak = ÅrsakTilOpprettelse.KLAGE
                ),
            )
            Triple(sak, førstegang, klage)
        }

        dataSource.transaction { connection ->
            val behandlingRepo = BehandlingRepositoryImpl(connection)

            // Hent ut igjen
            val alleDefault = behandlingRepo.hentAlleMedVedtakFor(sak.person)
            assertThat(alleDefault).hasSize(1)

            val alleFørstegang = behandlingRepo.hentAlleMedVedtakFor(sak.person, listOf(TypeBehandling.Førstegangsbehandling))
            assertThat(alleFørstegang).hasSize(1)
            assertThat(alleFørstegang[0].saksnummer).isEqualTo(sak.saksnummer)
            assertThat(alleFørstegang[0].referanse).isEqualTo(førstegang.referanse)
            assertThat(alleFørstegang[0].vedtakstidspunkt).isEqualTo(vedtakstidspunkt)
            assertThat(alleFørstegang[0].virkningstidspunkt).isEqualTo(virkningstidspunkt)
            assertThat(alleFørstegang[0].vurderingsbehov).isEqualTo(setOf(Vurderingsbehov.MOTTATT_SØKNAD))
            assertThat(alleFørstegang[0].årsakTilOpprettelse).isEqualTo(ÅrsakTilOpprettelse.SØKNAD)
        }
    }

    @Test
    fun `Kan hente saksnummer for behandling`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)
            val behandlingRepository = BehandlingRepositoryImpl(connection)

            val saksnummer = behandlingRepository.finnSaksnummer(behandling.referanse)

            assertThat(saksnummer).isEqualTo(sak.saksnummer)
        }
    }

    @Test
    fun `Kan hente vurderingbehovOgÅrsaker for behandling`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)

            // Legger til nye vurderingsbehov og årsak
            finnEllerOpprettBehandling(
                connection, sak,
                vurderingsbehov = listOf(
                    VurderingsbehovMedPeriode(
                        type = Vurderingsbehov.BARNETILLEGG
                    ),
                    VurderingsbehovMedPeriode(
                        type = Vurderingsbehov.REVURDER_MEDLEMSKAP
                    )
                ),
                årsakTilOpprettelse = ÅrsakTilOpprettelse.MANUELL_OPPRETTELSE
            )

            val behandlingRepository = BehandlingRepositoryImpl(connection)

            val vurderingsbehovOgÅrsaker = behandlingRepository.hentVurderingsbehovOgÅrsaker(behandling.id)

            val vurderingsbehovOgÅrsakSøknad = vurderingsbehovOgÅrsaker.find { it.årsak == ÅrsakTilOpprettelse.SØKNAD }
            assertThat(vurderingsbehovOgÅrsakSøknad).isNotNull
            assertThat(vurderingsbehovOgÅrsakSøknad?.vurderingsbehov?.map { it.type })
                .containsExactlyInAnyOrder(Vurderingsbehov.MOTTATT_SØKNAD)

            val vurderingbehovOgÅrsakManuellOpprettelse =
                vurderingsbehovOgÅrsaker.find { it.årsak == ÅrsakTilOpprettelse.MANUELL_OPPRETTELSE }
            assertThat(vurderingbehovOgÅrsakManuellOpprettelse).isNotNull
            assertThat(vurderingbehovOgÅrsakManuellOpprettelse?.vurderingsbehov?.map { it.type })
                .containsExactlyInAnyOrder(Vurderingsbehov.BARNETILLEGG, Vurderingsbehov.REVURDER_MEDLEMSKAP)
        }
    }

    @Test
    fun `Kan hente lagre duplikate vurderingsbehov`() {
        dataSource.transaction { connection ->
            val behandlingRepository = BehandlingRepositoryImpl(connection)

            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)

            // Simuler meldekort og korrigering av samme meldekort
            // Første meldekort
            finnEllerOpprettBehandling(
                connection, sak,
                vurderingsbehov = listOf(
                    VurderingsbehovMedPeriode(
                        type = Vurderingsbehov.MOTTATT_MELDEKORT,
                        periode = sak.rettighetsperiode,
                    ),
                ),
                årsakTilOpprettelse = ÅrsakTilOpprettelse.MELDEKORT
            )
            // Korrigering av meldekort (samme periode)
            finnEllerOpprettBehandling(
                connection, sak,
                vurderingsbehov = listOf(
                    VurderingsbehovMedPeriode(
                        type = Vurderingsbehov.MOTTATT_MELDEKORT,
                        periode = sak.rettighetsperiode,
                    ),
                ),
                årsakTilOpprettelse = ÅrsakTilOpprettelse.MELDEKORT
            )

            val vurderingsbehovOgÅrsaker = behandlingRepository.hentVurderingsbehovOgÅrsaker(behandling.id)
            assertThat(vurderingsbehovOgÅrsaker).hasSize(2)
            assertThat(vurderingsbehovOgÅrsaker.map { Pair(it.årsak, it.vurderingsbehov) })
                .containsExactlyInAnyOrder(
                    ÅrsakTilOpprettelse.SØKNAD to listOf(
                        VurderingsbehovMedPeriode(
                            type = Vurderingsbehov.MOTTATT_SØKNAD,
                            periode = null
                        )
                    ),
                    ÅrsakTilOpprettelse.MELDEKORT to listOf(
                        VurderingsbehovMedPeriode(
                            type = Vurderingsbehov.MOTTATT_MELDEKORT,
                            periode = sak.rettighetsperiode
                        )
                    )
                )
        }
    }

    private fun sak(connection: DBConnection): Sak {
        return PersonOgSakService(
            FakePdlGateway,
            PersonRepositoryImpl(connection),
            SakRepositoryImpl(connection)
        ).finnEllerOpprett(ident(), periode)
    }

}

// Midlertidig test
fun main() {
        val dataSource = TestDataSource()
        dataSource.transaction { connection ->
        BeregningsgrunnlagRepositoryImpl(connection).slett(
            BehandlingId(1L)
        )
        BehandlingRepositoryImpl(connection).slett(BehandlingId(1L))
        //AvklaringsbehovRepositoryImpl(connection).slett(BehandlingId(1L))
        BrevbestillingRepositoryImpl(connection).slett(BehandlingId(1L))
        TilkjentYtelseRepositoryImpl(connection).slett(BehandlingId(1L))
        VedtakRepositoryImpl(connection).slett(BehandlingId(1L))
        BehandlingRepositoryImpl(connection).slett(BehandlingId(1L))
        BarnetilleggRepositoryImpl(connection).slett(BehandlingId(1L))
        MeldeperiodeRepositoryImpl(connection).slett(BehandlingId(1L))
        TjenestePensjonRepositoryImpl(connection).slett(BehandlingId(1L))
        SamordningVurderingRepositoryImpl(connection).slett(BehandlingId(1L))
        SamordningAndreStatligeYtelserRepositoryImpl(connection).slett(BehandlingId(1L))
        SamordningRepositoryImpl(connection).slett(BehandlingId(1L))
        SamordningUføreRepositoryImpl(connection).slett(BehandlingId(1L))
        SamordningYtelseRepositoryImpl(connection).slett(BehandlingId(1L))
        UnderveisRepositoryImpl(connection).slett(BehandlingId(1L))
        VilkårsresultatRepositoryImpl(connection).slett(BehandlingId(1L))
        MeldekortRepositoryImpl(connection).slett(BehandlingId(1L))
        FormkravRepositoryImpl(connection).slett(BehandlingId(1L))
        MedlemskapArbeidInntektForutgåendeRepositoryImpl(connection).slett(BehandlingId(1L))
        MedlemskapArbeidInntektRepositoryImpl(connection).slett(BehandlingId(1L))
        PersonopplysningForutgåendeRepositoryImpl(connection).slett(BehandlingId(1L))
        PersonopplysningRepositoryImpl(connection).slett(BehandlingId(1L))
        BarnRepositoryImpl(connection).slett(BehandlingId(1L))
        InntektGrunnlagRepositoryImpl(connection).slett(BehandlingId(1L))
        InstitusjonsoppholdRepositoryImpl(connection).slett(BehandlingId(1L))
        UføreRepositoryImpl(connection).slett(BehandlingId(1L))
        YrkesskadeRepositoryImpl(connection).slett(BehandlingId(1L))
        ArbeidsevneRepositoryImpl(connection).slett(BehandlingId(1L))
        BeregningVurderingRepositoryImpl(connection).slett(BehandlingId(1L))
        BistandRepositoryImpl(connection).slett(BehandlingId(1L))
        MeldepliktRepositoryImpl(connection).slett(BehandlingId(1L))
        RefusjonkravRepositoryImpl(connection).slett(BehandlingId(1L))
        VurderRettighetsperiodeRepositoryImpl(connection).slett(BehandlingId(1L))
        StudentRepositoryImpl(connection).slett(BehandlingId(1L))
        SykdomRepositoryImpl(connection).slett(BehandlingId(1L))
        SykepengerErstatningRepositoryImpl(connection).slett(BehandlingId(1L))
        TrukketSøknadRepositoryImpl(connection).slett(BehandlingId(1L))
        ContextRepositoryImpl(connection).slett(BehandlingId(1L))
        TaSkriveLåsRepositoryImpl(connection).slett(BehandlingId(1L))
        PipRepositoryImpl(connection).slett(BehandlingId(1L))
        PersonRepositoryImpl(connection).slett(BehandlingId(1L))
        SakRepositoryImpl(connection).slett(BehandlingId(1L))
        ManuellInntektGrunnlagRepositoryImpl(connection).slett(BehandlingId(1L))

        dataSource.close()
    }

}