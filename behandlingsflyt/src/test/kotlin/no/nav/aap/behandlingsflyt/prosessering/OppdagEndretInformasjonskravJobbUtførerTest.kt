package no.nav.aap.behandlingsflyt.prosessering

import javax.sql.DataSource
import no.nav.aap.behandlingsflyt.faktagrunnlag.FakePdlGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.ForeldrepengerGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.ForeldrepengerRequest
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.ForeldrepengerResponse
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.SykepengerGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.SykepengerRequest
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.SykepengerResponse
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.Barn
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.BarnGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.BarnService
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.adapter.BarnInnhentingRespons
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
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
import no.nav.aap.behandlingsflyt.test.FakeUnleashFasttrackMeldekort
import no.nav.aap.behandlingsflyt.test.ident
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDatabase
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

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

        override fun hentBarn(
            person: Person,
            oppgitteBarnIdenter: List<Ident>
        ): BarnInnhentingRespons {
            return barnInnhentingRespons
        }
    }

    object FakeForeldrepengerGateway : ForeldrepengerGateway {
        override fun hentVedtakYtelseForPerson(request: ForeldrepengerRequest) = ForeldrepengerResponse(listOf())
    }

    object FakeSykepegerGateway : SykepengerGateway {
        override fun hentYtelseSykepenger(request: SykepengerRequest) = SykepengerResponse(listOf())
    }

    private val gatewayProvider = createGatewayProvider {
        register<FakeUnleashFasttrackMeldekort>()
        register<FakeBarnGateway>()
        register<FakePdlGateway>()
        register<FakeForeldrepengerGateway>()
        register<FakeSykepegerGateway>()
    }

    @TestDatabase
    lateinit var dataSource: DataSource


    @Test
    fun `endring i register fører til revurdering`() {
        val førstegangsbehandlingen = settOppFørstegangsvurdering()

        dataSource.transaction { connection ->
            val repositoryProvider = postgresRepositoryRegistry.provider(connection)
            FakeBarnGateway.barnInnhentingRespons = BarnInnhentingRespons(
                registerBarn = listOf(
                    Barn(
                        ident = Ident("11111"),
                        fødselsdato = Fødselsdato(1 januar 2010),
                    )
                ),
                oppgitteBarnFraPDL = listOf(),
            )

            val oppdagEndretInformasjonskravJobbUtfører = OppdagEndretInformasjonskravJobbUtfører.konstruer(
                repositoryProvider = repositoryProvider,
                gatewayProvider = gatewayProvider,
            )

            oppdagEndretInformasjonskravJobbUtfører.utfør(førstegangsbehandlingen.sakId, førstegangsbehandlingen.id)

            val sisteYtelsesbehandling =
                SakOgBehandlingService(repositoryProvider, gatewayProvider)
                    .finnSisteYtelsesbehandlingFor(førstegangsbehandlingen.sakId)!!
            assertThat(sisteYtelsesbehandling.id)
                .isNotEqualTo(førstegangsbehandlingen.id)
            assertThat(sisteYtelsesbehandling.vurderingsbehov())
                .isEqualTo(listOf(VurderingsbehovMedPeriode(Vurderingsbehov.BARNETILLEGG)))
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

            BarnService.konstruer(repositoryProvider, gatewayProvider).oppdater(
                FlytKontekstMedPerioder(
                    sakId = sak.id,
                    behandlingId = førstegangsbehandlingen.id,
                    forrigeBehandlingId = null,
                    behandlingType = TypeBehandling.Førstegangsbehandling,
                    vurderingType = VurderingType.FØRSTEGANGSBEHANDLING,
                    rettighetsperiode = sak.rettighetsperiode,
                    vurderingsbehovRelevanteForSteg = setOf()
                )
            )
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