package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.behandling.vedtak.VedtakRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskravkonstruktør
import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.tjenestepensjon.TjenestePensjonForhold
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.tjenestepensjon.TjenestePensjonInformasjonskrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.tjenestepensjon.gateway.TjenestePensjonGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelseVurderingInformasjonskrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.ForeldrepengerGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.ForeldrepengerRequest
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.ForeldrepengerResponse
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.SykepengerGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.SykepengerResponse
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.UtbetaltePerioder
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkår
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.BarnGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.BarnInformasjonskrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.adapter.BarnInnhentingRespons
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Institusjonsopphold
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.InstitusjonsoppholdGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.InstitusjonsoppholdInformasjonskrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Institusjonstype
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Oppholdstype
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonStatus
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Personopplysning
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonopplysningGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonopplysningInformasjonskrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonopplysningMedHistorikk
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.Uføre
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.UføreInformasjonskrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.UføreRegisterGateway
import no.nav.aap.behandlingsflyt.help.FakePdlGateway
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.help.genererVilkårsresultat
import no.nav.aap.behandlingsflyt.help.sak
import no.nav.aap.behandlingsflyt.integrasjon.createGatewayProvider
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.repository.postgresRepositoryRegistry
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.test.AlleAvskruddUnleash
import no.nav.aap.behandlingsflyt.test.fixedClock
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.komponenter.verdityper.Tid
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class OppdagEndretInformasjonskravJobbUtførerTest {
    init {
        JobbType.leggTil(ProsesserBehandlingJobbUtfører)
    }

    private val periode = Periode(1 januar 2020, 1 januar 2021)

    object FakeBarnGateway : BarnGateway {
        var barnInnhentingRespons = BarnInnhentingRespons(
            registerBarn = emptyList(),
            oppgitteBarnFraPDL = emptyList(),
            saksbehandlerOppgitteBarnPDL = emptyList()
        )

        override fun hentBarn(
            person: Person,
            oppgitteBarnIdenter: List<Ident>,
            saksbehandlerOppgitteBarnIdenter: List<Ident>
        ) = barnInnhentingRespons
    }

    object FakeForeldrepengerGateway : ForeldrepengerGateway {
        var response = ForeldrepengerResponse(emptyList())
        override fun hentVedtakYtelseForPerson(request: ForeldrepengerRequest) = response
    }

    object FakeSykepegerGateway : SykepengerGateway {
        var sykepengerRespons = SykepengerResponse(emptyList())
        override fun hentYtelseSykepenger(
            personidentifikatorer: Set<String>,
            fom: LocalDate,
            tom: LocalDate
        ): List<UtbetaltePerioder> {
            return sykepengerRespons.utbetaltePerioder
        }
    }

    object FakeTjenestePensjonGateway : TjenestePensjonGateway {
        override fun hentTjenestePensjon(ident: String, periode: Periode): List<TjenestePensjonForhold> = emptyList()
    }

    object FakeUføreRegisterGateway : UføreRegisterGateway {
        var response: Set<Uføre> = emptySet()
        override fun innhentMedHistorikk(person: Person, fraDato: LocalDate) = response
    }

    object FakeInstitusjonsoppholdGateway : InstitusjonsoppholdGateway {
        var response: List<Institusjonsopphold> = emptyList()
        override fun innhent(person: Person) = response
        override fun hentDataForHendelse(oppholdId: Long): Institusjonsopphold {
            TODO("Not yet implemented")
        }
    }

    object FakePersonopplysningGateway : PersonopplysningGateway {
        override fun innhent(person: Person): Personopplysning = Personopplysning(
            fødselsdato = Fødselsdato(1 januar 1990),
            status = PersonStatus.bosatt,
            statsborgerskap = emptyList()
        )

        override fun innhentMedHistorikk(person: Person): PersonopplysningMedHistorikk {
            TODO("Not yet implemented")
        }
    }

    private val gatewayProvider = createGatewayProvider {
        register<FakeBarnGateway>()
        register<FakePdlGateway>()
        register<FakeForeldrepengerGateway>()
        register<FakeSykepegerGateway>()
        register<FakeTjenestePensjonGateway>()
        register<FakeUføreRegisterGateway>()
        register<FakeInstitusjonsoppholdGateway>()
        register<FakePersonopplysningGateway>()
        register<AlleAvskruddUnleash>()
    }

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


    @Test
    fun `endring i register fører til revurdering og deduplisering av vurderingsbehov`() {
        val (sak, førstegangsbehandlingen) = settOppFørstegangsvurdering()

        dataSource.transaction { connection ->
            val repositoryProvider = postgresRepositoryRegistry.provider(connection)

            FakeSykepegerGateway.sykepengerRespons = SykepengerResponse(
                listOf(
                    UtbetaltePerioder(
                        fom = 10 januar 2020,
                        tom = 20 januar 2020,
                        grad = 100,
                        organisasjonsnummer = null
                    )
                )
            )
            FakeUføreRegisterGateway.response =
                setOf(
                    Uføre(
                        virkningstidspunkt = periode.fom,
                        uføregrad = Prosent.`100_PROSENT`,
                    )
                )
            FakeInstitusjonsoppholdGateway.response = listOf(
                Institusjonsopphold(
                    institusjonstype = Institusjonstype.FO,
                    kategori = Oppholdstype.A,
                    startdato = periode.fom,
                    sluttdato = periode.tom,
                    orgnr = "0".repeat(9),
                    institusjonsnavn = "hei"
                )
            )

            val klokke = fixedClock(sak.rettighetsperiode.fom.plusWeeks(2))

            OppdagEndretInformasjonskravJobbUtfører.konstruerMedKlokke(repositoryProvider, gatewayProvider, klokke)
                .utfør(førstegangsbehandlingen.sakId)

            val sisteYtelsesbehandling = SakOgBehandlingService(repositoryProvider, gatewayProvider)
                .finnSisteYtelsesbehandlingFor(førstegangsbehandlingen.sakId)!!
            assertThat(sisteYtelsesbehandling.id)
                .isNotEqualTo(førstegangsbehandlingen.id)
            assertThat(sisteYtelsesbehandling.vurderingsbehov()).hasSize(3)
            assertThat(sisteYtelsesbehandling.vurderingsbehov().toSet())
                .isEqualTo(
                    setOf(
                        VurderingsbehovMedPeriode(Vurderingsbehov.REVURDER_SAMORDNING_ANDRE_FOLKETRYGDYTELSER),
                        VurderingsbehovMedPeriode(Vurderingsbehov.REVURDER_SAMORDNING_UFØRE),
                        VurderingsbehovMedPeriode(Vurderingsbehov.INSTITUSJONSOPPHOLD),
                    )
                )
            assertThat(sisteYtelsesbehandling.typeBehandling())
                .isEqualTo(TypeBehandling.Revurdering)

            val jobber = repositoryProvider.provide<FlytJobbRepository>()
                .hentJobberForBehandling(sisteYtelsesbehandling.id.toLong())
                .filter { it.type() == ProsesserBehandlingJobbUtfører.type }
            assertThat(jobber).isNotEmpty
        }
    }

    @Test
    fun `ingen endring i register fører til ingen ny revurdering`() {
        val (sak, førstegangsbehandlingen) = settOppFørstegangsvurdering()
        val klokke = fixedClock(sak.rettighetsperiode.fom.plusWeeks(2))

        dataSource.transaction { connection ->
            val repositoryProvider = postgresRepositoryRegistry.provider(connection)

            val oppdagEndretInformasjonskravJobbUtfører = OppdagEndretInformasjonskravJobbUtfører.konstruerMedKlokke(
                repositoryProvider = repositoryProvider,
                gatewayProvider = gatewayProvider,
                klokke = klokke
            )

            oppdagEndretInformasjonskravJobbUtfører.utfør(førstegangsbehandlingen.sakId)

            val sisteYtelsesbehandling = SakOgBehandlingService(repositoryProvider, gatewayProvider)
                .finnSisteYtelsesbehandlingFor(førstegangsbehandlingen.sakId)
            assertThat(sisteYtelsesbehandling?.id)
                .isEqualTo(førstegangsbehandlingen.id)
        }
    }

    @Test
    fun `Kan sjekke om bruker har rett i en gitt periode`() {
        val rettighetsperiode = Periode(1 januar 2020, Tid.MAKS)

        val nå = 1 januar 2021
        
        val fulltAvslag = genererVilkårsresultat(
            periode,
            bistandVilkåret = Vilkår(
                Vilkårtype.BISTANDSVILKÅRET, setOf(
                    Vilkårsperiode(
                        rettighetsperiode,
                        Utfall.IKKE_OPPFYLT,
                        false,
                        null,
                        faktagrunnlag = null,
                        avslagsårsak = Avslagsårsak.IKKE_BEHOV_FOR_OPPFOLGING
                    )
                )
            )
        )

        val avslagPåAlderIGår = genererVilkårsresultat(
            periode,
            aldersVilkåret = Vilkår(
                Vilkårtype.ALDERSVILKÅRET, setOf(
                    Vilkårsperiode(
                        Periode(rettighetsperiode.fom, nå),
                        Utfall.OPPFYLT,
                        false,
                        null,
                        faktagrunnlag = null
                    ),
                    Vilkårsperiode(
                        Periode(nå.plusDays(1), Tid.MAKS),
                        Utfall.IKKE_OPPFYLT,
                        false,
                        null,
                        faktagrunnlag = null,
                        avslagsårsak = Avslagsårsak.BRUKER_OVER_67
                    )
                )
            )
        )

        val avslagPåAlderIDag = genererVilkårsresultat(
            periode,
            aldersVilkåret = Vilkår(
                Vilkårtype.ALDERSVILKÅRET, setOf(
                    Vilkårsperiode(
                        Periode(rettighetsperiode.fom, nå.minusDays(1)),
                        Utfall.OPPFYLT,
                        false,
                        null,
                        faktagrunnlag = null
                    ),
                    Vilkårsperiode(
                        Periode(nå, Tid.MAKS),
                        Utfall.IKKE_OPPFYLT,
                        false,
                        null,
                        faktagrunnlag = null,
                        avslagsårsak = Avslagsårsak.BRUKER_OVER_67
                    )
                )
            )
        )


        assertThat(
            OppdagEndretInformasjonskravJobbUtfører.harRettInnenforPeriode(
                Periode(
                    nå,
                    rettighetsperiode.tom
                ), avslagPåAlderIGår
            )
        ).isTrue()

        assertThat(
            OppdagEndretInformasjonskravJobbUtfører.harRettInnenforPeriode(
                Periode(
                    nå,
                    rettighetsperiode.tom
                ), avslagPåAlderIDag
            )
        ).isFalse()

        assertThat(
            OppdagEndretInformasjonskravJobbUtfører.harRettInnenforPeriode(
                Periode(
                    nå,
                    rettighetsperiode.tom
                ), fulltAvslag
            )
        ).isFalse()
    }

    private fun settOppFørstegangsvurdering(): Pair<Sak, Behandling> {
        return dataSource.transaction { connection ->
            val repositoryProvider = postgresRepositoryRegistry.provider(connection)
            val sak = sak(connection, periode)
            val førstegangsbehandlingen = finnEllerOpprettBehandling(connection, sak)

            val kontekst = FlytKontekstMedPerioder(
                sakId = sak.id,
                behandlingId = førstegangsbehandlingen.id,
                forrigeBehandlingId = null,
                behandlingType = TypeBehandling.Førstegangsbehandling,
                vurderingType = VurderingType.FØRSTEGANGSBEHANDLING,
                rettighetsperiode = sak.rettighetsperiode,
                vurderingsbehovRelevanteForSteg = emptySet()
            )

            fun klargjørInformasjonskrav(informasjonskrav: List<Informasjonskravkonstruktør>) {
                informasjonskrav.forEach {
                    val krav = it.konstruer(repositoryProvider, gatewayProvider)
                    val input = krav.klargjør(kontekst)
                    val data = krav.hentData(input)
                    krav.oppdater(input, data, kontekst)
                }
            }

            listOf(
                BarnInformasjonskrav,
                SamordningYtelseVurderingInformasjonskrav,
                TjenestePensjonInformasjonskrav,
                UføreInformasjonskrav,
                InstitusjonsoppholdInformasjonskrav,
                PersonopplysningInformasjonskrav
            ).let(::klargjørInformasjonskrav)


            repositoryProvider.provide<VilkårsresultatRepository>().lagre(
                førstegangsbehandlingen.id,
                genererVilkårsresultat(Periode(sak.rettighetsperiode.fom, Tid.MAKS))

            )
            repositoryProvider.provide<VedtakRepository>()
                .lagre(førstegangsbehandlingen.id, LocalDateTime.now(), LocalDate.now())
            repositoryProvider.provide<BehandlingRepository>()
                .oppdaterBehandlingStatus(førstegangsbehandlingen.id, Status.AVSLUTTET)
            Pair(sak, førstegangsbehandlingen)
        }
    }
}