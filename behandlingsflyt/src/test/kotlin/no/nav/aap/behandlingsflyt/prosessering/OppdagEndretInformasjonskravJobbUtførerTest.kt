package no.nav.aap.behandlingsflyt.prosessering

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
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonopplysningMedHistorikk
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonopplysningInformasjonskrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.Uføre
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.UføreRegisterGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.UføreInformasjonskrav
import no.nav.aap.behandlingsflyt.help.FakePdlGateway
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.integrasjon.createGatewayProvider
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.repository.postgresRepositoryRegistry
import no.nav.aap.behandlingsflyt.repository.sak.PersonRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.test.FakeUnleash
import no.nav.aap.behandlingsflyt.test.ident
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AutoClose
import org.junit.jupiter.api.Test
import java.time.LocalDate

class OppdagEndretInformasjonskravJobbUtførerTest {
    init {
        JobbType.leggTil(ProsesserBehandlingJobbUtfører)
    }

    private val periode = Periode(1 januar 2020, 1 januar 2021)

    object FakeBarnGateway : BarnGateway {
        var barnInnhentingRespons = BarnInnhentingRespons(
            registerBarn = emptyList(),
            oppgitteBarnFraPDL = emptyList(),
        )

        override fun hentBarn(person: Person, oppgitteBarnIdenter: List<Ident>) = barnInnhentingRespons
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
        var response: List<Uføre> = emptyList()
        override fun innhentMedHistorikk(person: Person, fraDato: LocalDate) = response
    }

    object FakeInstitusjonsoppholdGateway : InstitusjonsoppholdGateway {
        var response: List<Institusjonsopphold> = emptyList()
        override fun innhent(person: Person) = response
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
        register<FakeUnleash>()
    }

    @AutoClose
    private val dataSource = TestDataSource()


    @Test
    fun `endring i register fører til revurdering og deduplisering av vurderingsbehov`() {
        val førstegangsbehandlingen = settOppFørstegangsvurdering()

        dataSource.transaction { connection ->
            val repositoryProvider = postgresRepositoryRegistry.provider(connection)

            FakeSykepegerGateway.sykepengerRespons = SykepengerResponse(
                listOf(
                    UtbetaltePerioder(
                        fom = 10 januar 2020,
                        tom = 20 januar 2020,
                        grad = 100,
                    )
                )
            )
            FakeUføreRegisterGateway.response =
                listOf(
                    Uføre(
                        virkningstidspunkt = periode.fom,
                        uføregrad = Prosent.`100_PROSENT`,
                        kilde = "",
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

            OppdagEndretInformasjonskravJobbUtfører.konstruer(repositoryProvider, gatewayProvider)
                .utfør(førstegangsbehandlingen.sakId, førstegangsbehandlingen.id)

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
        val førstegangsbehandlingen = settOppFørstegangsvurdering()

        dataSource.transaction { connection ->
            val repositoryProvider = postgresRepositoryRegistry.provider(connection)

            val oppdagEndretInformasjonskravJobbUtfører = OppdagEndretInformasjonskravJobbUtfører.konstruer(
                repositoryProvider = repositoryProvider,
                gatewayProvider = gatewayProvider,
            )

            oppdagEndretInformasjonskravJobbUtfører.utfør(førstegangsbehandlingen.sakId, førstegangsbehandlingen.id)

            val sisteYtelsesbehandling = SakOgBehandlingService(repositoryProvider, gatewayProvider)
                .finnSisteYtelsesbehandlingFor(førstegangsbehandlingen.sakId)
            assertThat(sisteYtelsesbehandling?.id)
                .isEqualTo(førstegangsbehandlingen.id)
        }
    }

    private fun settOppFørstegangsvurdering(): Behandling {
        return dataSource.transaction { connection ->
            val repositoryProvider = postgresRepositoryRegistry.provider(connection)
            val sak = sak(connection)
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


            repositoryProvider.provide<BehandlingRepository>()
                .oppdaterBehandlingStatus(førstegangsbehandlingen.id, Status.AVSLUTTET)
            førstegangsbehandlingen
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