package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.register.barn

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.OppgitteBarn
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.VurderingAvForeldreAnsvar
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.VurdertBarn
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.repository.avklaringsbehov.FakePdlGateway
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.PersonRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.test.FreshDatabaseExtension
import no.nav.aap.behandlingsflyt.test.ident
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import javax.sql.DataSource

@ExtendWith(FreshDatabaseExtension::class)
internal class BarnRepositoryImplTest(val dataSource: DataSource) {

    @Test
    fun `Finner ikke barn hvis det ikke finnes barn`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)

            val barnRepository = BarnRepositoryImpl(connection)
            val barn = barnRepository.hentHvisEksisterer(behandling.id)
            assertThat(barn?.registerbarn?.identer).isNullOrEmpty()
        }
    }

    @Test
    fun `Lagrer og henter barn`() {
        val vurderteBarn = listOf(
            VurdertBarn(
                ident = Ident("12345"),
                vurderinger = listOf(
                    VurderingAvForeldreAnsvar(
                        fraDato = LocalDate.now(),
                        harForeldreAnsvar = true,
                        begrunnelse = "fsdf"
                    )
                )
            )
        )
        val barnListe = listOf(Ident("12345678910"), Ident("12345"))

        val behandling = dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)

            val barnRepository = BarnRepositoryImpl(connection)


            barnRepository.lagreRegisterBarn(behandling.id, barnListe)
            barnRepository.lagreOppgitteBarn(behandling.id, OppgitteBarn(identer = listOf(Ident("1"))))
            barnRepository.lagreVurderinger(behandling.id, "ident", vurderteBarn)
            behandling
        }

        val barn = dataSource.transaction { connection ->
            val barnRepository = BarnRepositoryImpl(connection)
            barnRepository.hent(behandling.id)
        }

        assertThat(barn.registerbarn?.identer).containsExactlyInAnyOrderElementsOf(barnListe)
        assertThat(barn.oppgitteBarn?.identer).containsExactly(Ident("1"))
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
        dataSource.transaction {
            BarnRepositoryImpl(it).lagreRegisterBarn(behandling.id, listOf(Ident("12"), Ident("32323")))
        }

        val uthentet = dataSource.transaction {
            BarnRepositoryImpl(it).hent(behandling.id)
        }

        assertThat(uthentet.registerbarn?.identer).containsExactlyInAnyOrder(Ident("12"), Ident("32323"))
    }

    @Test
    fun `oppdatering av oppgitte barn`() {
        val behandling = dataSource.transaction { connection ->
            val sak = sak(connection)
            finnEllerOpprettBehandling(connection, sak)
        }

        dataSource.transaction {
            BarnRepositoryImpl(it).lagreOppgitteBarn(behandling.id, OppgitteBarn(identer = listOf(Ident("1"))))
        }

        val uthentet = dataSource.transaction {
            BarnRepositoryImpl(it).hent(behandling.id)
        }

        assertThat(uthentet.oppgitteBarn?.identer).containsExactly(Ident("1"))

        // Oppdater med ingen oppgitte barn
        dataSource.transaction {
            BarnRepositoryImpl(it).lagreOppgitteBarn(behandling.id, OppgitteBarn(identer = emptyList()))
        }

        val uthentet2 = dataSource.transaction {
            BarnRepositoryImpl(it).hent(behandling.id)
        }

        assertThat(uthentet2.oppgitteBarn?.identer).isNullOrEmpty()
    }

    @Test
    fun `Kopiering av barn fra en behandling til en annen`() {
        dataSource.transaction { connection ->
            val barnRepository = BarnRepositoryImpl(connection)
            // Given
            val sak = sak(connection)
            val gammelBehandling = finnEllerOpprettBehandling(connection, sak)
            barnRepository.lagreOppgitteBarn(gammelBehandling.id, OppgitteBarn(identer = listOf(Ident("1"))))

            // When
            BehandlingRepositoryImpl(connection).oppdaterBehandlingStatus(gammelBehandling.id, Status.AVSLUTTET)
            val nyBehandling = finnEllerOpprettBehandling(connection, sak)
            //Finn eller opprett behandling kopierer også

            // Then
            val gamleOppgitteBarn = barnRepository.hent(nyBehandling.id).oppgitteBarn?.identer
            val nyeOppgitteBarn = barnRepository.hent(nyBehandling.id).oppgitteBarn?.identer
            assertThat(nyeOppgitteBarn).isEqualTo(gamleOppgitteBarn)
        }
    }

    private companion object {
        private val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))
    }

    private fun sak(connection: DBConnection): Sak {
        return PersonOgSakService(
            FakePdlGateway,
            PersonRepositoryImpl(connection),
            SakRepositoryImpl(connection)
        ).finnEllerOpprett(
            ident(),
            periode
        )
    }
}