package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.register.barn

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.Barn
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.Dødsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.OppgitteBarn
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.Relasjon
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.SaksbehandlerOppgitteBarn
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.BarnIdentifikator
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.VurderingAvForeldreAnsvar
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.VurdertBarn
import no.nav.aap.behandlingsflyt.help.FakePdlGateway
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.PersonRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
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

internal class BarnRepositoryImplTest {

    private companion object {
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
    fun `Finner ikke barn hvis det ikke finnes barn`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)

            val barnRepository = BarnRepositoryImpl(connection)
            val barn = barnRepository.hentHvisEksisterer(behandling.id)
            assertThat(barn?.registerbarn?.barn).isNullOrEmpty()
        }
    }

    @Test
    fun `Lagrer og henter barn`() {
        val vurderteBarn = listOf(
            VurdertBarn(
                ident = BarnIdentifikator.BarnIdent("12345"), vurderinger = listOf(
                    VurderingAvForeldreAnsvar(
                        fraDato = LocalDate.now(), harForeldreAnsvar = true, begrunnelse = "fsdf"
                    )
                )
            ), VurdertBarn(
                ident = BarnIdentifikator.NavnOgFødselsdato("Olof Olof", Fødselsdato(LocalDate.now().minusYears(10))),
                vurderinger = listOf(
                    VurderingAvForeldreAnsvar(
                        fraDato = LocalDate.now(), harForeldreAnsvar = true, begrunnelse = "fsdf"
                    )
                )
            )
        )
        val barnListe = listOf(
            BarnIdentifikator.BarnIdent("12345678910"), BarnIdentifikator.BarnIdent("12345"),
        ).map {
            Barn(
                it, Fødselsdato(LocalDate.now().minusYears(10)), Dødsdato(LocalDate.now().minusYears(5))
            )
        } + listOf(
            Barn(
                BarnIdentifikator.NavnOgFødselsdato("Gorgo Grog", Fødselsdato(LocalDate.now().minusYears(10))),
                Fødselsdato(LocalDate.now().minusYears(10)),
                navn = "Gorgo Grog",
            )
        )


        val oppgittBarn = OppgitteBarn.OppgittBarn(
            Ident("1"),
            "John Johnsen",
            Fødselsdato(LocalDate.now().minusYears(13)),
            Relasjon.FOSTERFORELDER
        )

        val behandling = dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)

            val barnRepository = BarnRepositoryImpl(connection)


            barnRepository.lagreRegisterBarn(
                behandling.id,
                barnListe.associateWith {
                    val ident = it.ident
                    when (ident) {
                        is BarnIdentifikator.BarnIdent -> PersonRepositoryImpl(connection).finnEllerOpprett(listOf(ident.ident)).id
                        is BarnIdentifikator.NavnOgFødselsdato -> null
                    }
                })
            barnRepository.lagreOppgitteBarn(
                behandling.id, OppgitteBarn(oppgitteBarn = listOf(oppgittBarn))
            )
            barnRepository.lagreVurderinger(behandling.id, "ident", vurderteBarn)
            behandling
        }

        val barn = dataSource.transaction { connection ->
            val barnRepository = BarnRepositoryImpl(connection)
            barnRepository.hent(behandling.id)
        }

        assertThat(barn.registerbarn?.barn).containsExactlyInAnyOrderElementsOf(barnListe)
        assertThat(barn.oppgitteBarn?.oppgitteBarn).containsExactly(oppgittBarn)
        assertThat(barn.vurderteBarn?.barn).isEqualTo(vurderteBarn)

        dataSource.transaction { connection ->

            val barnRepository = BarnRepositoryImpl(connection)
            // Slette
            barnRepository.slett(behandling.id)
            assertThat(barnRepository.hentHvisEksisterer(behandling.id)).isNull()

        }
    }

    @Test
    fun `lagre kun registerbarn`() {
        val behandling = dataSource.transaction { connection ->
            val sak = sak(connection)
            finnEllerOpprettBehandling(connection, sak)
        }
        val barnListe =
            listOf(Ident("12"), Ident("32323")).map {
                Barn(
                    BarnIdentifikator.BarnIdent(it),
                    Fødselsdato(LocalDate.now().minusYears(10))
                )
            }
        dataSource.transaction { connection ->
            BarnRepositoryImpl(connection).lagreRegisterBarn(
                behandling.id,
                barnListe.associateWith { PersonRepositoryImpl(connection).finnEllerOpprett(listOf((it.ident as BarnIdentifikator.BarnIdent).ident)).id })
        }

        val uthentet = dataSource.transaction {
            BarnRepositoryImpl(it).hent(behandling.id)
        }

        assertThat(uthentet.registerbarn?.barn).containsExactlyInAnyOrderElementsOf(barnListe)
    }

    @Test
    fun `oppdatering av oppgitte barn`() {
        val behandling = dataSource.transaction { connection ->
            val sak = sak(connection)
            finnEllerOpprettBehandling(connection, sak)
        }

        val oppgittBarn = OppgitteBarn.OppgittBarn(
            ident = Ident("1"),
            navn = "Robert Rokko",
            fødselsdato = Fødselsdato(LocalDate.now()),
        )

        dataSource.transaction {
            BarnRepositoryImpl(it).lagreOppgitteBarn(
                behandling.id, OppgitteBarn(oppgitteBarn = listOf(oppgittBarn))
            )
        }

        val uthentet = dataSource.transaction {
            BarnRepositoryImpl(it).hent(behandling.id)
        }

        assertThat(uthentet.oppgitteBarn?.oppgitteBarn).containsExactly(
            OppgitteBarn.OppgittBarn(
                ident = Ident("1"),
                navn = "Robert Rokko",
                fødselsdato = Fødselsdato(LocalDate.now()),
            )
        )

        // Oppdater med ingen oppgitte barn
        dataSource.transaction {
            BarnRepositoryImpl(it).lagreOppgitteBarn(behandling.id, OppgitteBarn(oppgitteBarn = emptyList()))
        }

        val uthentet2 = dataSource.transaction {
            BarnRepositoryImpl(it).hent(behandling.id)
        }

        assertThat(uthentet2.oppgitteBarn?.oppgitteBarn).isNullOrEmpty()
    }

    @Test
    fun `Kopiering av barn fra en behandling til en annen`() {
        dataSource.transaction { connection ->
            val barnRepository = BarnRepositoryImpl(connection)
            // Given
            val sak = sak(connection)
            val gammelBehandling = finnEllerOpprettBehandling(connection, sak)
            barnRepository.lagreOppgitteBarn(
                gammelBehandling.id,
                OppgitteBarn(oppgitteBarn = listOf(OppgitteBarn.OppgittBarn(ident = Ident("1"), null)))
            )

            // When
            BehandlingRepositoryImpl(connection).oppdaterBehandlingStatus(gammelBehandling.id, Status.AVSLUTTET)
            val nyBehandling = finnEllerOpprettBehandling(connection, sak)
            //Finn eller opprett behandling kopierer også

            // Then
            val gamleOppgitteBarn = barnRepository.hent(nyBehandling.id).oppgitteBarn?.oppgitteBarn
            val nyeOppgitteBarn = barnRepository.hent(nyBehandling.id).oppgitteBarn?.oppgitteBarn
            assertThat(nyeOppgitteBarn).isEqualTo(gamleOppgitteBarn)
        }
    }

    @Test
    fun `Lagre kun saksbehandler oppgitte barn`() {
        val behandling = dataSource.transaction { connection ->
            val sak = sak(connection)
            finnEllerOpprettBehandling(connection, sak)
        }
        val saksbehandlerOppgittBarn = SaksbehandlerOppgitteBarn.SaksbehandlerOppgitteBarn(
            ident = Ident("123456"),
            navn = "Mini Mus",
            fødselsdato = Fødselsdato(LocalDate.now().minusYears(5)),
            relasjon = Relasjon.FORELDER
        )

        dataSource.transaction {
            BarnRepositoryImpl(it).lagreSaksbehandlerOppgitteBarn(
                behandling.id, listOf(saksbehandlerOppgittBarn)
            )
        }

        val uthentet = dataSource.transaction {
            BarnRepositoryImpl(it).hent(behandling.id)
        }

        assertThat(uthentet.saksbehandlerOppgitteBarn?.barn).containsExactly(saksbehandlerOppgittBarn)
    }

    private fun sak(connection: DBConnection): Sak {
        return PersonOgSakService(
            FakePdlGateway, PersonRepositoryImpl(connection), SakRepositoryImpl(connection)
        ).finnEllerOpprett(
            ident(), periode
        )
    }
}
