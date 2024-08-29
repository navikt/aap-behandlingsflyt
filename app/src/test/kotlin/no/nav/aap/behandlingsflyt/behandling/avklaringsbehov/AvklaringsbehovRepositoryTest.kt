package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov

import kotlinx.coroutines.runBlocking
import no.nav.aap.behandlingsflyt.SYSTEMBRUKER
import no.nav.aap.behandlingsflyt.dbtestdata.ident
import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.EndringType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Årsak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.IdentGateway
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.verdityper.flyt.StegType
import no.nav.aap.verdityper.sakogbehandling.Ident
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class AvklaringsbehovRepositoryTest {

    @Test
    fun `løs avklaringsbehov skal avslutte avklaringsbehovet`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val sak = runBlocking { sak(connection) }
            val behandling = behandling(connection, sak)
            val repository = AvklaringsbehovRepositoryImpl(connection)
            val avklaringsbehovene = Avklaringsbehovene(repository, behandling.id)
            avklaringsbehovene.leggTil(
                listOf(Definisjon.AVKLAR_SYKDOM), StegType.AVKLAR_SYKDOM, begrunnelse = "", bruker = SYSTEMBRUKER
            )

            val avklaringsbehov = repository.hentAvklaringsbehovene(behandling.id)
            assertThat(avklaringsbehov.alle()).hasSize(1)
            assertThat(avklaringsbehov.alle()[0].erAvsluttet()).isFalse()

            avklaringsbehovene.løsAvklaringsbehov(
                definisjon = Definisjon.AVKLAR_SYKDOM,
                begrunnelse = "Godkjent",
                endretAv = "Saksbehandler",
                kreverToTrinn = true
            )

            val avklaringsbehovEtterLøst = repository.hentAvklaringsbehovene(behandling.id)
            assertThat(avklaringsbehovEtterLøst.alle()[0].erAvsluttet()).isTrue()
        }
    }

    private companion object {
        private val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))
    }

    private fun sak(connection: DBConnection): Sak {
        return PersonOgSakService(connection, FakePdlGateway).finnEllerOpprett(ident(), periode)
    }

    private fun behandling(connection: DBConnection, sak: Sak): Behandling {
        return SakOgBehandlingService(connection).finnEllerOpprettBehandling(
            sak.saksnummer,
            listOf(Årsak(EndringType.MOTTATT_SØKNAD))
        ).behandling
    }

    @Test
    fun hentAvklaringsbehovene() {
        // TODO implementer
    }

    @Test
    fun kreverToTrinn() {
        // TODO implementer
    }

    @Test
    fun opprett() {
        // TODO implementer
    }

    @Test
    fun endre() {
        // TODO implementer
    }

    @Test
    fun endreVentepunkt() {
        // TODO implementer
    }
}

object FakePdlGateway : IdentGateway {
    override fun hentAlleIdenterForPerson(ident: Ident): List<Ident> {
        return listOf(ident)
    }
}