package no.nav.aap.behandlingsflyt.faktagrunnlag

import no.nav.aap.behandlingsflyt.behandling.lovvalg.LovvalgInformasjonskrav
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.MeldepliktStatus
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.ArbeidsGradering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.Underveisperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepositoryImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.BarnInformasjonskrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonStatus
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Personopplysning
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Statsborgerskap
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.YrkesskadeInformasjonskrav
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.help.flytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.help.ident
import no.nav.aap.behandlingsflyt.help.opprettRevurdering
import no.nav.aap.behandlingsflyt.help.opprettSak
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
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.underveis.UnderveisRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.medlemskaplovvalg.MedlemskapArbeidInntektRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.personopplysning.PersonopplysningRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.register.yrkesskade.YrkesskadeRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.postgresRepositoryRegistry
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingService
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.behandlingsflyt.test.AlleAvskruddUnleash
import no.nav.aap.behandlingsflyt.test.FakePersoner
import no.nav.aap.behandlingsflyt.test.FakeTidligereVurderinger
import no.nav.aap.behandlingsflyt.test.Fakes
import no.nav.aap.behandlingsflyt.test.modell.TestPerson
import no.nav.aap.behandlingsflyt.test.modell.TestYrkesskade
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Dagsatser
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.komponenter.verdityper.TimerArbeid
import no.nav.aap.lookup.repository.RepositoryProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.time.LocalDate

@Fakes
class InformasjonskravGrunnlagTest {

    companion object {
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

    private val gatewayProvider = createGatewayProvider {
        register<MedlemskapGateway>()
        register<AARegisterGateway>()
        register<EREGGateway>()
        register<YrkesskadeRegisterGatewayImpl>()
        register<PdlBarnGateway>()
        register<PdlIdentGateway>()
        register<AlleAvskruddUnleash>()
        register<InntektkomponentenGatewayImpl>()
    }

    @Test
    fun `Yrkesskadedata er oppdatert`() {
        dataSource.transaction { connection ->
            val (ident, kontekst) = klargjør(connection)
            val repositoryProvider = postgresRepositoryRegistry.provider(connection)
            val informasjonskravGrunnlag = InformasjonskravGrunnlagImpl(
                InformasjonskravRepositoryImpl(connection),
                repositoryProvider,
                gatewayProvider
            )

            FakePersoner.leggTil(
                TestPerson(
                    identer = setOf(ident),
                    fødselsdato = Fødselsdato(LocalDate.now().minusYears(20)),
                    yrkesskade = listOf(TestYrkesskade())
                )
            )

            val yrkesskadeInformasjonskravKonstruktørMedTidligereVurderingerFake = genererKonstruktørMedFake(connection)


            val initiell = informasjonskravGrunnlag.oppdaterFaktagrunnlagForKravliste(
                listOf(StegType.VURDER_YRKESSKADE to yrkesskadeInformasjonskravKonstruktørMedTidligereVurderingerFake),
                kontekst
            )

            assertThat(initiell)
                .hasSize(1)
                .allMatch { it === yrkesskadeInformasjonskravKonstruktørMedTidligereVurderingerFake }

            val erOppdatert = informasjonskravGrunnlag.oppdaterFaktagrunnlagForKravliste(
                listOf(StegType.VURDER_YRKESSKADE to yrkesskadeInformasjonskravKonstruktørMedTidligereVurderingerFake),
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
                InformasjonskravRepositoryImpl(connection),
                postgresRepositoryRegistry.provider(connection),
                gatewayProvider
            )

            val yrkesskadeInformasjonskravKonstruktørMedTidligereVurderingerFake = genererKonstruktørMedFake(connection)

            FakePersoner.leggTil(
                TestPerson(
                    identer = setOf(ident),
                    fødselsdato = Fødselsdato(LocalDate.now().minusYears(20)),
                    yrkesskade = listOf(TestYrkesskade())
                )
            )

            val erOppdatert = informasjonskravGrunnlag.oppdaterFaktagrunnlagForKravliste(
                listOf(StegType.VURDER_YRKESSKADE to yrkesskadeInformasjonskravKonstruktørMedTidligereVurderingerFake),
                kontekst
            )

            assertThat(erOppdatert)
                .hasSize(1)
                .allMatch { it === yrkesskadeInformasjonskravKonstruktørMedTidligereVurderingerFake }
        }
    }

    @Test
    fun `Yrkesskadedata utdatert uten endring, oppgittYrkesskadeISoeknad lagres alltid`() {
        dataSource.transaction { connection ->
            val (_, kontekst) = klargjør(connection)
            val informasjonskravGrunnlag = InformasjonskravGrunnlagImpl(
                InformasjonskravRepositoryImpl(connection),
                postgresRepositoryRegistry.provider(connection),
                gatewayProvider
            )

            val yrkesskadeInformasjonskravKonstruktørMedTidligereVurderingerFake = genererKonstruktørMedFake(connection)

            val erOppdatert = informasjonskravGrunnlag.oppdaterFaktagrunnlagForKravliste(
                listOf(StegType.VURDER_YRKESSKADE to yrkesskadeInformasjonskravKonstruktørMedTidligereVurderingerFake),
                kontekst
            )

            assertThat(erOppdatert).isNotEmpty()
        }
    }

    @Test
    fun `Lovvalg og medlemskap er oppdatert`() {
        val (ident, kontekst, lovvalgKonstruktør) = dataSource.transaction { connection ->
            val (ident, kontekst) = klargjør(connection)
            Triple(ident, kontekst, genererLovvalgKonstruktørMedFake())
        }

        FakePersoner.leggTil(
            TestPerson(
                identer = setOf(ident),
                fødselsdato = Fødselsdato(LocalDate.now().minusYears(20)),
                yrkesskade = emptyList()
            )
        )

        val initiell = dataSource.transaction {
            InformasjonskravGrunnlagImpl(
                postgresRepositoryRegistry.provider(it),
                gatewayProvider
            ).oppdaterFaktagrunnlagForKravliste(
                listOf(StegType.VURDER_LOVVALG to lovvalgKonstruktør),
                kontekst
            )
        }
        assertThat(initiell)
            .hasSize(1)
            .allMatch { it === lovvalgKonstruktør }

        val erOppdatert = dataSource.transaction {
            InformasjonskravGrunnlagImpl(
                postgresRepositoryRegistry.provider(it),
                gatewayProvider
            ).oppdaterFaktagrunnlagForKravliste(
                listOf(StegType.VURDER_LOVVALG to lovvalgKonstruktør),
                kontekst
            )
        }
        val lagretData = dataSource.transaction {
            MedlemskapArbeidInntektRepositoryImpl(it).hentHvisEksisterer(kontekst.behandlingId)
        }

        assertThat(lagretData?.inntekterINorgeGrunnlag!!).hasSizeGreaterThan(0)
        assertThat(erOppdatert).isEmpty()

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
            val barnKonstruktør = genererBarnKonstruktørMedFake()
            val kravKonstruktører = listOf(StegType.BARNETILLEGG to barnKonstruktør)

            leggTilBarnPåPerson(ident)

            val initiell = informasjonskravGrunnlag.oppdaterFaktagrunnlagForKravliste(kravKonstruktører, kontekst)

            assertThat(initiell)
                .hasSize(1)
                .allMatch { it === barnKonstruktør }

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
            val barnKonstruktør = genererBarnKonstruktørMedFake()
            val kravKonstruktører = listOf(StegType.BARNETILLEGG to barnKonstruktør)

            leggTilBarnPåPerson(ident)

            val initiell = informasjonskravGrunnlag.oppdaterFaktagrunnlagForKravliste(kravKonstruktører, kontekst)

            assertThat(initiell)
                .hasSize(1)
                .allMatch { it === barnKonstruktør }

            val erOppdatert = informasjonskravGrunnlag.oppdaterFaktagrunnlagForKravliste(kravKonstruktører, kontekst)

            assertThat(erOppdatert).isEmpty()
        }
    }

    @Test
    fun `Revurdering etter avslag medfører også oppdatering av barn fra registeret`() {
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
            val barnKonstruktør = genererBarnKonstruktørMedFake()
            val kravKonstruktører = listOf(StegType.BARNETILLEGG to barnKonstruktør)

            leggTilBarnPåPerson(ident)

            val initiell = informasjonskravGrunnlag.oppdaterFaktagrunnlagForKravliste(kravKonstruktører, kontekst)

            assertThat(initiell).hasSize(1)
        }
    }


    @Test
    fun `Revurdering med årsak annen enn barnetillegg medfører ingen oppdatering av barn fra registeret`() {
        dataSource.transaction { connection ->
            val ident = ident()
            val sak = opprettSak(connection, ident, LocalDate.now())
            val underveisRepository = UnderveisRepositoryImpl(connection)

            val førstegangsbehandling = finnEllerOpprettBehandling(connection, sak)
            PersonopplysningRepositoryImpl(connection).lagre(
                førstegangsbehandling.id,
                Personopplysning(
                    Fødselsdato(LocalDate.now().minusYears(20)), status = PersonStatus.bosatt, statsborgerskap = listOf(
                        Statsborgerskap("NOR")
                    )
                )
            )

            // Simuler tidligere innvilget AAP på førstegangsbehandlingen
            val rettighetsperiode = Periode(LocalDate.now(), LocalDate.now().plusYears(1))
            simulerRettPåAAP(underveisRepository, førstegangsbehandling, rettighetsperiode)

            val revurdering = opprettRevurdering(
                connection, sak,
                vurderingsbehov = listOf(VurderingsbehovMedPeriode(Vurderingsbehov.REVURDER_MEDLEMSKAP))
            )

            val kontekst = flytKontekstMedPerioder {
                this.behandling = revurdering
                vurderingsbehovRelevanteForSteg = setOf(Vurderingsbehov.REVURDER_MEDLEMSKAP)
                this.rettighetsperiode = rettighetsperiode
            }

            val informasjonskravGrunnlag = InformasjonskravGrunnlagImpl(
                InformasjonskravRepositoryImpl(connection),
                postgresRepositoryRegistry.provider(connection),
                gatewayProvider
            )

            val barnKonstruktør = genererBarnKonstruktørMedFake()
            val kravKonstruktører = listOf(StegType.BARNETILLEGG to barnKonstruktør)

            leggTilBarnPåPerson(ident)

            val initiell = informasjonskravGrunnlag.oppdaterFaktagrunnlagForKravliste(kravKonstruktører, kontekst)

            assertThat(initiell).hasSize(0)
        }
    }

    private fun simulerRettPåAAP(
        underveisRepository: UnderveisRepositoryImpl,
        førstegangsbehandling: Behandling,
        rettighetsperiode: Periode
    ) {
        underveisRepository.lagre(
            førstegangsbehandling.id, listOf(
                Underveisperiode(
                    periode = rettighetsperiode,
                    meldePeriode = rettighetsperiode,
                    utfall = Utfall.OPPFYLT,
                    rettighetsType = RettighetsType.BISTANDSBEHOV,
                    avslagsårsak = null,
                    grenseverdi = Prosent.`50_PROSENT`,
                    institusjonsoppholdReduksjon = Prosent.`70_PROSENT`,
                    arbeidsgradering = ArbeidsGradering(
                        TimerArbeid(0.toBigDecimal()),
                        Prosent.`50_PROSENT`,
                        Prosent.`50_PROSENT`,
                        Prosent.`50_PROSENT`,
                        LocalDate.now()
                    ),
                    trekk = Dagsatser(0),
                    brukerAvKvoter = setOf(),
                    meldepliktStatus = MeldepliktStatus.MELDT_SEG,
                    meldepliktGradering = Prosent.`0_PROSENT`
                )
            ),
            input = object : Faktagrunnlag {}
        )
    }

    private fun klargjør(
        connection: DBConnection,
        vurderingType: VurderingType = VurderingType.FØRSTEGANGSBEHANDLING,
        årsakerTilBehandling: Set<Vurderingsbehov> = setOf(Vurderingsbehov.MOTTATT_SØKNAD)

    ): Pair<Ident, FlytKontekstMedPerioder> {
        val ident = ident()
        val sak = opprettSak(connection, ident, LocalDate.now())
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

        return ident to flytKontekstMedPerioder {
            this.behandling = behandling
            this.vurderingType = vurderingType
            vurderingsbehovRelevanteForSteg = årsakerTilBehandling
            rettighetsperiode = Periode(LocalDate.now(), LocalDate.now())
        }
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

    private fun genererKonstruktørMedFake(connection: DBConnection) =

        object : Informasjonskravkonstruktør {
            override val navn: InformasjonskravNavn
                get() = YrkesskadeInformasjonskrav.navn

            override fun konstruer(
                repositoryProvider: RepositoryProvider,
                gatewayProvider: GatewayProvider
            ): YrkesskadeInformasjonskrav {
                return YrkesskadeInformasjonskrav(
                    sakService = SakService(
                        repositoryProvider,
                        gatewayProvider
                    ),
                    yrkesskadeRepository = YrkesskadeRepositoryImpl(connection),
                    personopplysningRepository = PersonopplysningRepositoryImpl(connection),
                    yrkesskadeRegisterGateway = gatewayProvider.provide(),
                    mottattDokumentRepository = MottattDokumentRepositoryImpl(connection),
                    tidligereVurderinger = FakeTidligereVurderinger()
                )
            }
        }

    private fun genererLovvalgKonstruktørMedFake() =
        object : Informasjonskravkonstruktør {
            override val navn: InformasjonskravNavn
                get() = LovvalgInformasjonskrav.navn

            override fun konstruer(
                repositoryProvider: RepositoryProvider,
                gatewayProvider: GatewayProvider
            ): LovvalgInformasjonskrav {
                return LovvalgInformasjonskrav(
                    sakService = SakService(repositoryProvider, gatewayProvider),
                    medlemskapArbeidInntektRepository = repositoryProvider.provide(),
                    medlemskapRepository = repositoryProvider.provide(),
                    tidligereVurderinger = FakeTidligereVurderinger(),
                    medlemskapGateway = gatewayProvider.provide(),
                    arbeidsForholdGateway = gatewayProvider.provide(),
                    enhetsregisteretGateway = gatewayProvider.provide(),
                    inntektskomponentenGateway = gatewayProvider.provide(),
                )
            }
        }

    private fun genererBarnKonstruktørMedFake() =
        object : Informasjonskravkonstruktør {
            override val navn: InformasjonskravNavn
                get() = BarnInformasjonskrav.navn

            override fun konstruer(
                repositoryProvider: RepositoryProvider,
                gatewayProvider: GatewayProvider
            ): BarnInformasjonskrav {
                return BarnInformasjonskrav(
                    barnRepository = repositoryProvider.provide(),
                    personRepository = repositoryProvider.provide(),
                    barnGateway = gatewayProvider.provide(),
                    identGateway = gatewayProvider.provide(),
                    tidligereVurderinger = FakeTidligereVurderinger(),
                    behandlingService = BehandlingService(repositoryProvider, gatewayProvider),
                    sakService = SakService(repositoryProvider, gatewayProvider),
                )
            }
        }
}
