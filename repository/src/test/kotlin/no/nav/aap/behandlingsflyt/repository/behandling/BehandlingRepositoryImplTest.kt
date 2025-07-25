package no.nav.aap.behandlingsflyt.repository.behandling

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.BeregningsgrunnlagRepositoryImpl
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.repository.avklaringsbehov.FakePdlGateway
import no.nav.aap.behandlingsflyt.repository.behandling.brev.bestilling.BrevbestillingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.behandling.tilkjentytelse.TilkjentYtelseRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.behandling.vedtak.VedtakRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.barnetillegg.BarnetilleggRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.effektuer11_7.Effektuer11_7RepositoryImpl
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
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Årsak
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.ÅrsakTilBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.test.ident
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

internal class BehandlingRepositoryImplTest {
    companion object {
        private val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))
        private val dataSource = InitTestDatabase.freshDatabase()

        @AfterAll
        @JvmStatic
        fun afterAll() {
            InitTestDatabase.closerFor(dataSource)
        }
    }


    @Test
    fun `kan lagre og hente ut behandling med uuid`() {
        val skapt = dataSource.transaction { connection ->
            val sak = PersonOgSakService(
                FakePdlGateway,
                PersonRepositoryImpl(connection),
                SakRepositoryImpl(connection)
            ).finnEllerOpprett(
                ident(),
                Periode(LocalDate.now(), LocalDate.now().plusYears(3))
            )
            val repo = BehandlingRepositoryImpl(connection)

            // Opprett
            repo.opprettBehandling(
                sakId = sak.id,
                årsaker = listOf(Årsak(type = ÅrsakTilBehandling.MOTTATT_SØKNAD)),
                typeBehandling = TypeBehandling.Førstegangsbehandling,
                forrigeBehandlingId = null
            )
        }

        dataSource.transaction { connection ->
            val repo = BehandlingRepositoryImpl(connection)

            // Hent ut igjen
            val hententMedReferanse = repo.hent(skapt.referanse)

            assertThat(hententMedReferanse.referanse).isEqualTo(skapt.referanse)
            assertThat(hententMedReferanse.årsaker()).containsExactlyElementsOf(skapt.årsaker())
            assertThat(hententMedReferanse.årsaker()).containsExactlyElementsOf(listOf(Årsak(type = ÅrsakTilBehandling.MOTTATT_SØKNAD)))
            assertThat(hententMedReferanse.typeBehandling()).isEqualTo(TypeBehandling.Førstegangsbehandling)
        }
    }

    @Test
    fun `oppretet dato lagres på behandling og hentes ut korrekt`() {
        val skapt = dataSource.transaction { connection ->
            val sak = PersonOgSakService(
                FakePdlGateway,
                PersonRepositoryImpl(connection),
                SakRepositoryImpl(connection)
            ).finnEllerOpprett(
                ident(),
                Periode(LocalDate.now(), LocalDate.now().plusYears(3))
            )
            val repo = BehandlingRepositoryImpl(connection)

            // Opprett
            repo.opprettBehandling(
                sakId = sak.id,
                årsaker = listOf(Årsak(type = ÅrsakTilBehandling.MOTTATT_SØKNAD)),
                typeBehandling = TypeBehandling.Førstegangsbehandling,
                forrigeBehandlingId = null
            )
        }

        dataSource.transaction { connection ->
            val repo = BehandlingRepositoryImpl(connection)

            // Hent ut igjen
            val hententMedReferanse = repo.hent(skapt.referanse)

            assertThat(hententMedReferanse.opprettetTidspunkt).isEqualTo(skapt.opprettetTidspunkt)
        }
    }

    @Test
    fun `kan hente ut behandlinger for sak filtrert på type`() {
        val (sak, førstegang, klage) = dataSource.transaction { connection ->
            val sak = PersonOgSakService(
                FakePdlGateway,
                PersonRepositoryImpl(connection),
                SakRepositoryImpl(connection)
            ).finnEllerOpprett(
                ident(),
                Periode(LocalDate.now(), LocalDate.now().plusYears(3))
            )
            val repo = BehandlingRepositoryImpl(connection)

            // Opprett
            val førstegang = repo.opprettBehandling(
                sakId = sak.id,
                årsaker = listOf(Årsak(type = ÅrsakTilBehandling.MOTTATT_SØKNAD)),
                typeBehandling = TypeBehandling.Førstegangsbehandling,
                forrigeBehandlingId = null
            )

            val klage = repo.opprettBehandling(
                sakId = sak.id,
                årsaker = listOf(Årsak(type = ÅrsakTilBehandling.MOTATT_KLAGE)),
                typeBehandling = TypeBehandling.Klage,
                forrigeBehandlingId = null
            )
            Triple(sak, førstegang, klage)
        }

        dataSource.transaction { connection ->
            val repo = BehandlingRepositoryImpl(connection)

            // Hent ut igjen
            val alleDefault = repo.hentAlleFor(sak.id)
            assertThat(alleDefault).hasSize(2)

            val alleFørstegang = repo.hentAlleFor(sak.id, listOf(TypeBehandling.Førstegangsbehandling))
            assertThat(alleFørstegang).hasSize(1)
            assertThat(alleFørstegang[0].referanse).isEqualTo(førstegang.referanse)

            val alleKlage = repo.hentAlleFor(sak.id, listOf(TypeBehandling.Klage))
            assertThat(alleKlage).hasSize(1)
            assertThat(alleKlage[0].referanse).isEqualTo(klage.referanse)
        }
    }

    @Test
    fun `kan hente ut behandlinger med vedtak for person`() {
        val vedtakstidspunkt = LocalDateTime.now()
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
            val repo = BehandlingRepositoryImpl(connection)
            val vedtakRepo = VedtakRepositoryImpl(connection)

            // Opprett
            val førstegang = repo.opprettBehandling(
                sakId = sak.id,
                årsaker = listOf(Årsak(type = ÅrsakTilBehandling.MOTTATT_SØKNAD)),
                typeBehandling = TypeBehandling.Førstegangsbehandling,
                forrigeBehandlingId = null
            )

            vedtakRepo.lagre(
                behandlingId = førstegang.id,
                vedtakstidspunkt = vedtakstidspunkt,
                virkningstidspunkt = virkningstidspunkt,
            )

            val klage = repo.opprettBehandling(
                sakId = sak.id,
                årsaker = listOf(Årsak(type = ÅrsakTilBehandling.MOTATT_KLAGE)),
                typeBehandling = TypeBehandling.Klage,
                forrigeBehandlingId = null
            )
            Triple(sak, førstegang, klage)
        }

        dataSource.transaction { connection ->
            val repo = BehandlingRepositoryImpl(connection)

            // Hent ut igjen
            val alleDefault = repo.hentAlleMedVedtakFor(sak.person)
            assertThat(alleDefault).hasSize(1)

            val alleFørstegang = repo.hentAlleMedVedtakFor(sak.person, listOf(TypeBehandling.Førstegangsbehandling))
            assertThat(alleFørstegang).hasSize(1)
            assertThat(alleFørstegang[0].saksnummer).isEqualTo(sak.saksnummer)
            assertThat(alleFørstegang[0].referanse).isEqualTo(førstegang.referanse)
            assertThat(alleFørstegang[0].vedtakstidspunkt).isEqualToIgnoringNanos(vedtakstidspunkt)
            assertThat(alleFørstegang[0].virkningstidspunkt).isEqualTo(virkningstidspunkt)
            assertThat(alleFørstegang[0].årsaker).isEqualTo(setOf(ÅrsakTilBehandling.MOTTATT_SØKNAD))
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
    InitTestDatabase.freshDatabase().transaction { connection ->
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
        Effektuer11_7RepositoryImpl(connection).slett(BehandlingId(1L))
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
        PersonopplysningForutgåendeRepositoryImpl(connection, PersonRepositoryImpl(connection)).slett(BehandlingId(1L))
        PersonopplysningRepositoryImpl(connection, PersonRepositoryImpl(connection)).slett(BehandlingId(1L))
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
    }

}