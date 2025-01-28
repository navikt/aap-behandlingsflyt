package no.nav.aap.behandlingsflyt.faktagrunnlag

import no.nav.aap.behandlingsflyt.behandling.lovvalg.LovvalgService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.BeregningsgrunnlagRepositoryImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepositoryImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Personopplysning
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.register.yrkesskade.YrkesskadeRepositoryImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.YrkesskadeService
import no.nav.aap.behandlingsflyt.repository.avklaringsbehov.AvklaringsbehovRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.medlemskaplovvalg.MedlemskapArbeidInntektRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.personopplysning.PersonopplysningRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.lås.TaSkriveLåsRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.pip.PipRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.PersonRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Årsak
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.ÅrsakTilBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.PersonStatus
import no.nav.aap.behandlingsflyt.test.FakePersoner
import no.nav.aap.behandlingsflyt.test.Fakes
import no.nav.aap.behandlingsflyt.test.ident
import no.nav.aap.behandlingsflyt.test.modell.TestPerson
import no.nav.aap.behandlingsflyt.test.modell.TestYrkesskade
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.lookup.repository.RepositoryRegistry
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

@Fakes
class InformasjonskravGrunnlagTest {

    companion object {
        private val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))
    }

    @BeforeEach
    fun setUp() {
        RepositoryRegistry.register(BehandlingRepositoryImpl::class)
            .register(PersonRepositoryImpl::class)
            .register(SakRepositoryImpl::class)
            .register(AvklaringsbehovRepositoryImpl::class)
            .register(VilkårsresultatRepositoryImpl::class)
            .register(PipRepositoryImpl::class)
            .register(TaSkriveLåsRepositoryImpl::class)
            .register(BeregningsgrunnlagRepositoryImpl::class)
            .register(PersonopplysningRepositoryImpl::class)
            .register<MottattDokumentRepositoryImpl>()
            .register<YrkesskadeRepositoryImpl>()
            .register<MedlemskapArbeidInntektRepositoryImpl>()
    }

    @Test
    fun `Yrkesskadedata er oppdatert`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val (ident, kontekst) = klargjør(connection)
            val informasjonskravGrunnlag = InformasjonskravGrunnlagImpl(connection)

            FakePersoner.leggTil(
                TestPerson(
                    identer = setOf(ident),
                    fødselsdato = Fødselsdato(LocalDate.now().minusYears(20)),
                    yrkesskade = listOf(TestYrkesskade())
                )
            )

            val initiell = informasjonskravGrunnlag.oppdaterFaktagrunnlagForKravliste(
                listOf(YrkesskadeService),
                kontekst
            )

            assertThat(initiell)
                .hasSize(1)
                .allMatch { it === YrkesskadeService }

            val erOppdatert = informasjonskravGrunnlag.oppdaterFaktagrunnlagForKravliste(
                listOf(YrkesskadeService),
                kontekst
            )

            assertThat(erOppdatert).isEmpty()
        }
    }

    @Test
    fun `Yrkesskadedata er ikke oppdatert`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val (ident, kontekst) = klargjør(connection)
            val informasjonskravGrunnlag = InformasjonskravGrunnlagImpl(connection)

            FakePersoner.leggTil(
                TestPerson(
                    identer = setOf(ident),
                    fødselsdato = Fødselsdato(LocalDate.now().minusYears(20)),
                    yrkesskade = listOf(TestYrkesskade())
                )
            )

            val erOppdatert = informasjonskravGrunnlag.oppdaterFaktagrunnlagForKravliste(
                listOf(YrkesskadeService),
                kontekst
            )

            assertThat(erOppdatert)
                .hasSize(1)
                .allMatch { it === YrkesskadeService }
        }
    }

    @Test
    fun `Yrkesskadedata er utdatert, men har ingen endring fra registeret`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val (_, kontekst) = klargjør(connection)
            val informasjonskravGrunnlag = InformasjonskravGrunnlagImpl(connection)

            val erOppdatert = informasjonskravGrunnlag.oppdaterFaktagrunnlagForKravliste(
                listOf(YrkesskadeService),
                kontekst
            )

            assertThat(erOppdatert).isEmpty()
        }
    }

    @Test
    fun LovvalgMedlemskapErOppdatert() {
        InitTestDatabase.dataSource.transaction { connection ->
            val (ident, kontekst) = klargjør(connection)
            val informasjonskravGrunnlag = InformasjonskravGrunnlagImpl(connection)

            FakePersoner.leggTil(
                TestPerson(
                    identer = setOf(ident),
                    fødselsdato = Fødselsdato(LocalDate.now().minusYears(20)),
                    yrkesskade = listOf()
                )
            )

            val initiell = informasjonskravGrunnlag.oppdaterFaktagrunnlagForKravliste(
                listOf(LovvalgService),
                kontekst
            )

            assertThat(initiell)
                .hasSize(1)
                .allMatch { it === LovvalgService }

            val erOppdatert = informasjonskravGrunnlag.oppdaterFaktagrunnlagForKravliste(
                listOf(LovvalgService),
                kontekst
            )

            assertThat(erOppdatert).isEmpty()
        }
    }

    private fun klargjør(connection: DBConnection): Pair<Ident, FlytKontekstMedPerioder> {
        val ident = ident()
        val sak = PersonOgSakService(
            FakePdlGateway,
            PersonRepositoryImpl(connection),
            SakRepositoryImpl(connection)
        ).finnEllerOpprett(ident, periode)
        val behandling = SakOgBehandlingService(
            GrunnlagKopierer(connection), SakRepositoryImpl(connection),
            BehandlingRepositoryImpl(connection)
        ).finnEllerOpprettBehandling(
            sak.saksnummer,
            listOf(Årsak(ÅrsakTilBehandling.MOTTATT_SØKNAD))
        ).behandling
        val personopplysningRepository = PersonopplysningRepositoryImpl(
            connection,
            PersonRepositoryImpl(connection)
        )
        personopplysningRepository.lagre(behandling.id, Personopplysning(Fødselsdato(LocalDate.now().minusYears(20)), land = "NOR", status = PersonStatus.bosatt))

        val flytKontekst = behandling.flytKontekst()
        return ident to FlytKontekstMedPerioder(
            flytKontekst.sakId,
            flytKontekst.behandlingId,
            behandling.typeBehandling(),
            setOf(
                Vurdering(
                    VurderingType.FØRSTEGANGSBEHANDLING,
                    listOf(ÅrsakTilBehandling.MOTTATT_SØKNAD),
                    Periode(LocalDate.now(), LocalDate.now())
                )
            )
        )
    }
}