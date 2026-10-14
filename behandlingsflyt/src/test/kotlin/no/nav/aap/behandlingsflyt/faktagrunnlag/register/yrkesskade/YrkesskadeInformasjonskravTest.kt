package no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade

import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav.Endret.ENDRET
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav.Endret.IKKE_ENDRET
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepositoryImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.YrkesskadeInformasjonskrav.YrkesskadeInput
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.YrkesskadeInformasjonskrav.YrkesskadeRegisterdata
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.help.flytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.help.ident
import no.nav.aap.behandlingsflyt.help.opprettSak
import no.nav.aap.behandlingsflyt.integrasjon.createGatewayProvider
import no.nav.aap.behandlingsflyt.integrasjon.yrkesskade.YrkesskadeRegisterGatewayImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.personopplysning.PersonopplysningRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.register.yrkesskade.YrkesskadeRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.postgresRepositoryRegistry
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.behandlingsflyt.test.FakeTidligereVurderinger
import no.nav.aap.behandlingsflyt.test.Fakes
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.time.LocalDate

@Fakes
class YrkesskadeInformasjonskravTest {

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
        register<YrkesskadeRegisterGatewayImpl>()
    }

    private val fødselsdato = Fødselsdato(LocalDate.now().minusYears(20))

    private fun yrkesskade(skadedato: LocalDate = LocalDate.of(2019, 6, 4)) = Yrkesskade(
        ref = "ref",
        saksnummer = 123,
        kildesystem = "INFOTRYGD",
        skadedato = skadedato,
        vedtaksdato = LocalDate.of(2020, 5, 1),
        skadeart = "Arbeidsulykke",
        diagnose = "Lumbago",
        skadekombinasjoner = emptyList(),
        skadekombinasjonerTekst = null,
    )

    private class Oppsett(
        val krav: YrkesskadeInformasjonskrav,
        val repository: YrkesskadeRepositoryImpl,
        val input: YrkesskadeInput,
        val kontekst: FlytKontekstMedPerioder,
    )

    private fun oppsett(connection: DBConnection): Oppsett {
        val sak: Sak = opprettSak(connection, ident(), LocalDate.now())
        val behandling = finnEllerOpprettBehandling(connection, sak)
        val repositoryProvider = postgresRepositoryRegistry.provider(connection)
        val repository = YrkesskadeRepositoryImpl(connection)
        val krav = YrkesskadeInformasjonskrav(
            sakService = SakService(repositoryProvider, gatewayProvider),
            yrkesskadeRepository = repository,
            personopplysningRepository = PersonopplysningRepositoryImpl(connection),
            yrkesskadeRegisterGateway = gatewayProvider.provide(),
            mottattDokumentRepository = MottattDokumentRepositoryImpl(connection),
            tidligereVurderinger = FakeTidligereVurderinger(),
        )
        val kontekst = flytKontekstMedPerioder {
            this.behandling = behandling
            rettighetsperiode = Periode(LocalDate.now(), LocalDate.now())
        }
        return Oppsett(krav, repository, YrkesskadeInput(sak.person, fødselsdato), kontekst)
    }

    @Test
    fun `ingen gamle data og tom liste fra register gir ENDRET første gang og lagrer grunnlag`() {
        dataSource.transaction { connection ->
            val o = oppsett(connection)

            val endret = o.krav.oppdater(o.input, YrkesskadeRegisterdata(emptyList()), o.kontekst)

            assertThat(endret).isEqualTo(ENDRET)
            val grunnlag = o.repository.hentHvisEksisterer(o.kontekst.behandlingId)
            assertThat(grunnlag).isNotNull
            assertThat(grunnlag!!.yrkesskader.yrkesskader).isEmpty()
            assertThat(grunnlag.oppgittYrkesskadeISøknad).isFalse()
        }
    }

    @Test
    fun `gjentatt tom liste fra register gir IKKE_ENDRET og samme grunnlag`() {
        dataSource.transaction { connection ->
            val o = oppsett(connection)
            val tom = YrkesskadeRegisterdata(emptyList())

            o.krav.oppdater(o.input, tom, o.kontekst)
            val førsteId = o.repository.hentHvisEksisterer(o.kontekst.behandlingId)!!.id

            val endret = o.krav.oppdater(o.input, tom, o.kontekst)

            assertThat(endret).isEqualTo(IKKE_ENDRET)
            assertThat(o.repository.hentHvisEksisterer(o.kontekst.behandlingId)!!.id).isEqualTo(førsteId)
        }
    }

    @Test
    fun `samme yrkesskader to ganger gir IKKE_ENDRET`() {
        dataSource.transaction { connection ->
            val o = oppsett(connection)
            val data = YrkesskadeRegisterdata(listOf(yrkesskade()))

            assertThat(o.krav.oppdater(o.input, data, o.kontekst)).isEqualTo(ENDRET)
            assertThat(o.krav.oppdater(o.input, data, o.kontekst)).isEqualTo(IKKE_ENDRET)
        }
    }

    @Test
    fun `endret skadedato gir ENDRET`() {
        dataSource.transaction { connection ->
            val o = oppsett(connection)

            o.krav.oppdater(o.input, YrkesskadeRegisterdata(listOf(yrkesskade())), o.kontekst)
            val endret = o.krav.oppdater(
                o.input,
                YrkesskadeRegisterdata(listOf(yrkesskade(LocalDate.of(2019, 5, 4)))),
                o.kontekst
            )

            assertThat(endret).isEqualTo(ENDRET)
        }
    }

    @Test
    fun `yrkesskader som forsvinner fra register gir ENDRET`() {
        dataSource.transaction { connection ->
            val o = oppsett(connection)

            o.krav.oppdater(o.input, YrkesskadeRegisterdata(listOf(yrkesskade())), o.kontekst)
            val endret = o.krav.oppdater(o.input, YrkesskadeRegisterdata(emptyList()), o.kontekst)

            assertThat(endret).isEqualTo(ENDRET)
            assertThat(o.repository.hentHvisEksisterer(o.kontekst.behandlingId)!!.yrkesskader.yrkesskader).isEmpty()
        }
    }

    @Test
    fun `yrkesskader som dukker opp etter tomt register gir ENDRET`() {
        dataSource.transaction { connection ->
            val o = oppsett(connection)

            o.krav.oppdater(o.input, YrkesskadeRegisterdata(emptyList()), o.kontekst)
            val endret = o.krav.oppdater(o.input, YrkesskadeRegisterdata(listOf(yrkesskade())), o.kontekst)

            assertThat(endret).isEqualTo(ENDRET)
        }
    }
}