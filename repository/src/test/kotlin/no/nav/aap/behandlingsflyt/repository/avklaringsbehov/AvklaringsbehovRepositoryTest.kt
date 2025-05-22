package no.nav.aap.behandlingsflyt.repository.avklaringsbehov

import no.nav.aap.behandlingsflyt.SYSTEMBRUKER
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.behandling.søknad.TrukketSøknadService
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.PersonRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.IdentGateway
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.test.ident
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryAvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryTrukketSøknadRepository
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class AvklaringsbehovRepositoryTest {

    @Test
    fun `løs avklaringsbehov skal avslutte avklaringsbehovet`() {
        InitTestDatabase.freshDatabase().transaction { connection ->
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)
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
        return PersonOgSakService(
            FakePdlGateway,
            PersonRepositoryImpl(connection),
            SakRepositoryImpl(connection),
            BehandlingRepositoryImpl(connection),
            TrukketSøknadService(
                InMemoryAvklaringsbehovRepository,
                InMemoryTrukketSøknadRepository
            ),
        ).finnEllerOpprett(ident(), periode)
    }
}

object FakePdlGateway : IdentGateway {
    override fun hentAlleIdenterForPerson(ident: Ident): List<Ident> {
        return listOf(ident)
    }
}