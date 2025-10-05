package no.nav.aap.behandlingsflyt.faktagrunnlag

import no.nav.aap.behandlingsflyt.behandling.lovvalg.LovvalgInformasjonskrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.BarnInformasjonskrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonStatus
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Personopplysning
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Statsborgerskap
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.YrkesskadeInformasjonskrav
import no.nav.aap.behandlingsflyt.help.FakePdlGateway
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.integrasjon.aordning.InntektkomponentenGatewayImpl
import no.nav.aap.behandlingsflyt.integrasjon.arbeidsforhold.AARegisterGateway
import no.nav.aap.behandlingsflyt.integrasjon.arbeidsforhold.EREGGateway
import no.nav.aap.behandlingsflyt.integrasjon.createGatewayProvider
import no.nav.aap.behandlingsflyt.integrasjon.ident.PdlIdentGateway
import no.nav.aap.behandlingsflyt.integrasjon.medlemsskap.MedlemskapGateway
import no.nav.aap.behandlingsflyt.integrasjon.pdl.PdlBarnGateway
import no.nav.aap.behandlingsflyt.integrasjon.yrkesskade.YrkesskadeRegisterGatewayImpl
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.InformasjonskravRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.medlemskaplovvalg.MedlemskapArbeidInntektRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.personopplysning.PersonopplysningRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.postgresRepositoryRegistry
import no.nav.aap.behandlingsflyt.repository.sak.PersonRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.test.FakePersoner
import no.nav.aap.behandlingsflyt.test.FakeUnleash
import no.nav.aap.behandlingsflyt.test.Fakes
import no.nav.aap.behandlingsflyt.test.ident
import no.nav.aap.behandlingsflyt.test.modell.TestPerson
import no.nav.aap.behandlingsflyt.test.modell.TestYrkesskade
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import java.time.LocalDate

@Fakes
class InformasjonskravGrunnlagTest {

    companion object {
        private val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))
        private val dataSource = InitTestDatabase.freshDatabase()

        @AfterAll
        @JvmStatic
        fun afterAll() {
            InitTestDatabase.closerFor(dataSource)
        }
    }
    
    private val gatewayProvider = createGatewayProvider {
        register<MedlemskapGateway>()
        register<AARegisterGateway>()
        register<EREGGateway>()
        register<YrkesskadeRegisterGatewayImpl>()
        register<PdlBarnGateway>()
        register<PdlIdentGateway>()
        register<FakeUnleash>()
        register<InntektkomponentenGatewayImpl>()
    }

    @Test
    fun `Yrkesskadedata er oppdatert`() {
        dataSource.transaction { connection ->
            val (ident, kontekst) = klargjør(connection)
            val informasjonskravGrunnlag = InformasjonskravGrunnlagImpl(
                InformasjonskravRepositoryImpl(connection), postgresRepositoryRegistry.provider(connection), gatewayProvider
            )

            FakePersoner.leggTil(
                TestPerson(
                    identer = setOf(ident),
                    fødselsdato = Fødselsdato(LocalDate.now().minusYears(20)),
                    yrkesskade = listOf(TestYrkesskade())
                )
            )

            val initiell = informasjonskravGrunnlag.oppdaterFaktagrunnlagForKravliste(
                listOf(StegType.VURDER_YRKESSKADE to YrkesskadeInformasjonskrav),
                kontekst
            )

            assertThat(initiell)
                .hasSize(1)
                .allMatch { it === YrkesskadeInformasjonskrav }

            val erOppdatert = informasjonskravGrunnlag.oppdaterFaktagrunnlagForKravliste(
                listOf(StegType.VURDER_YRKESSKADE to YrkesskadeInformasjonskrav),
                kontekst
            )

            assertThat(erOppdatert).isEmpty()
        }
    }

    @Test
    fun `Yrkesskadedata er ikke oppdatert`() {
        dataSource.transaction { connection ->
            val (ident, kontekst) = klargjør(connection)
            val informasjonskravGrunnlag = InformasjonskravGrunnlagImpl(
                InformasjonskravRepositoryImpl(connection), postgresRepositoryRegistry.provider(connection), gatewayProvider
            )

            FakePersoner.leggTil(
                TestPerson(
                    identer = setOf(ident),
                    fødselsdato = Fødselsdato(LocalDate.now().minusYears(20)),
                    yrkesskade = listOf(TestYrkesskade())
                )
            )

            val erOppdatert = informasjonskravGrunnlag.oppdaterFaktagrunnlagForKravliste(
                listOf(StegType.VURDER_YRKESSKADE to YrkesskadeInformasjonskrav),
                kontekst
            )

            assertThat(erOppdatert)
                .hasSize(1)
                .allMatch { it === YrkesskadeInformasjonskrav }
        }
    }

    @Test
    fun `Yrkesskadedata er utdatert, men har ingen endring fra registeret`() {
        dataSource.transaction { connection ->
            val (_, kontekst) = klargjør(connection)
            val informasjonskravGrunnlag = InformasjonskravGrunnlagImpl(
                InformasjonskravRepositoryImpl(connection), postgresRepositoryRegistry.provider(connection), gatewayProvider
            )

            val erOppdatert = informasjonskravGrunnlag.oppdaterFaktagrunnlagForKravliste(
                listOf(StegType.VURDER_YRKESSKADE to YrkesskadeInformasjonskrav),
                kontekst
            )

            assertThat(erOppdatert).isEmpty()
        }
    }

    @Test
    fun `Lovvalg og medlemskap er oppdatert`() {
        dataSource.transaction { connection ->
            val (ident, kontekst) = klargjør(connection)
            val informasjonskravGrunnlag = InformasjonskravGrunnlagImpl(
                InformasjonskravRepositoryImpl(connection), postgresRepositoryRegistry.provider(connection), gatewayProvider
            )

            FakePersoner.leggTil(
                TestPerson(
                    identer = setOf(ident),
                    fødselsdato = Fødselsdato(LocalDate.now().minusYears(20)),
                    yrkesskade = emptyList()
                )
            )

            val initiell = informasjonskravGrunnlag.oppdaterFaktagrunnlagForKravliste(
                listOf(StegType.VURDER_LOVVALG to LovvalgInformasjonskrav),
                kontekst
            )

            assertThat(initiell)
                .hasSize(1)
                .allMatch { it === LovvalgInformasjonskrav }

            val erOppdatert = informasjonskravGrunnlag.oppdaterFaktagrunnlagForKravliste(
                listOf(StegType.VURDER_LOVVALG to LovvalgInformasjonskrav),
                kontekst
            )
            val lagretData = MedlemskapArbeidInntektRepositoryImpl(connection).hentHvisEksisterer(kontekst.behandlingId)

            assertThat(lagretData?.inntekterINorgeGrunnlag?.size == 2).isTrue()
            assertThat(erOppdatert).isEmpty()
        }
    }

    @Test
    fun `Førstegangsbehandling medfører henting av barn fra registeret`() {
        dataSource.transaction { connection ->
            val (ident, kontekst) = klargjør(connection, VurderingType.FØRSTEGANGSBEHANDLING)
            val informasjonskravGrunnlag = InformasjonskravGrunnlagImpl(
                InformasjonskravRepositoryImpl(connection),
                postgresRepositoryRegistry.provider(connection),
                gatewayProvider
            )
            val kravKonstruktører = listOf(StegType.BARNETILLEGG to BarnInformasjonskrav)

            leggTilBarnPåPerson(ident)

            val initiell = informasjonskravGrunnlag.oppdaterFaktagrunnlagForKravliste(kravKonstruktører, kontekst)

            assertThat(initiell)
                .hasSize(1)
                .allMatch { it === BarnInformasjonskrav }

            val erOppdatert = informasjonskravGrunnlag.oppdaterFaktagrunnlagForKravliste(kravKonstruktører, kontekst)

            assertThat(erOppdatert).isEmpty()
        }
    }

    @Test
    fun `Revurdering med årsak barnetillegg medfører ny henting av barn fra registeret`() {
        dataSource.transaction { connection ->
            val (ident, kontekst) = klargjør(
                connection,
                VurderingType.REVURDERING,
                setOf(Vurderingsbehov.BARNETILLEGG)
            )
            val informasjonskravGrunnlag = InformasjonskravGrunnlagImpl(
                InformasjonskravRepositoryImpl(connection),
                postgresRepositoryRegistry.provider(connection),
                gatewayProvider
            )
            val kravKonstruktører = listOf(StegType.BARNETILLEGG to BarnInformasjonskrav)

            leggTilBarnPåPerson(ident)

            val initiell = informasjonskravGrunnlag.oppdaterFaktagrunnlagForKravliste(kravKonstruktører, kontekst)

            assertThat(initiell)
                .hasSize(1)
                .allMatch { it === BarnInformasjonskrav }

            val erOppdatert = informasjonskravGrunnlag.oppdaterFaktagrunnlagForKravliste(kravKonstruktører, kontekst)

            assertThat(erOppdatert).isEmpty()
        }
    }

    @Test
    fun `Revurdering med årsak annen enn barnetillegg medfører ingen oppdatering av barn fra registeret`() {
        dataSource.transaction { connection ->
            val (ident, kontekst) = klargjør(
                connection,
                VurderingType.REVURDERING,
                setOf(Vurderingsbehov.REVURDER_MEDLEMSKAP)
            )
            val informasjonskravGrunnlag = InformasjonskravGrunnlagImpl(
                InformasjonskravRepositoryImpl(connection),
                postgresRepositoryRegistry.provider(connection),
                gatewayProvider
            )
            val kravKonstruktører = listOf(StegType.BARNETILLEGG to BarnInformasjonskrav)

            leggTilBarnPåPerson(ident)

            val initiell = informasjonskravGrunnlag.oppdaterFaktagrunnlagForKravliste(kravKonstruktører, kontekst)

            assertThat(initiell).hasSize(0)
        }
    }

    private fun klargjør(
        connection: DBConnection,
        vurderingType: VurderingType = VurderingType.FØRSTEGANGSBEHANDLING,
        årsakerTilBehandling: Set<Vurderingsbehov> = setOf(Vurderingsbehov.MOTTATT_SØKNAD)

    ): Pair<Ident, FlytKontekstMedPerioder> {
        val ident = ident()
        val sak = PersonOgSakService(
            FakePdlGateway,
            PersonRepositoryImpl(connection),
            SakRepositoryImpl(connection)
        ).finnEllerOpprett(ident, periode)
        val behandling = finnEllerOpprettBehandling(connection, sak)
        val personopplysningRepository = PersonopplysningRepositoryImpl(connection)
        personopplysningRepository.lagre(
            behandling.id,
            Personopplysning(
                Fødselsdato(LocalDate.now().minusYears(20)), status = PersonStatus.bosatt, statsborgerskap = listOf(
                    Statsborgerskap("NOR")
                )
            )
        )

        val flytKontekst = behandling.flytKontekst()
        return ident to FlytKontekstMedPerioder(
            flytKontekst.sakId,
            flytKontekst.behandlingId,
            flytKontekst.forrigeBehandlingId,
            behandling.typeBehandling(),
            vurderingType = vurderingType,
            vurderingsbehovRelevanteForSteg = årsakerTilBehandling,
            rettighetsperiode = Periode(LocalDate.now(), LocalDate.now()),
        )
    }

    private fun leggTilBarnPåPerson(ident: Ident) {
        FakePersoner.leggTil(
            TestPerson(
                identer = setOf(ident),
                barn = listOf(
                    TestPerson(
                        fødselsdato = Fødselsdato(LocalDate.now().minusYears(5)),
                    )
                )
            )
        )
    }
}
