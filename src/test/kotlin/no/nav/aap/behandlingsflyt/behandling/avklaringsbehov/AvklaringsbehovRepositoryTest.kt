package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov

import no.nav.aap.behandlingsflyt.Periode
import no.nav.aap.behandlingsflyt.avklaringsbehov.AvklaringsbehovRepositoryImpl
import no.nav.aap.behandlingsflyt.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.behandling.Behandling
import no.nav.aap.behandlingsflyt.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.behandling.behandlingRepository
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.dbconnect.InitTestDatabase
import no.nav.aap.behandlingsflyt.dbconnect.transaction
import no.nav.aap.behandlingsflyt.flyt.steg.StegType
import no.nav.aap.behandlingsflyt.ident
import no.nav.aap.behandlingsflyt.sak.PersonRepository
import no.nav.aap.behandlingsflyt.sak.Sak
import no.nav.aap.behandlingsflyt.sak.sakRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class AvklaringsbehovRepositoryTest {

    @Test
    fun `løs avklaringsbehov skal avslutte avklaringsbehovet`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = behandling(connection, sak)
            val repository = AvklaringsbehovRepositoryImpl(connection)
            val avklaringsbehovene = Avklaringsbehovene(repository, behandling.id)
            avklaringsbehovene.leggTil(
                listOf(Definisjon.AVKLAR_SYKDOM), StegType.AVKLAR_SYKDOM
            )

            val avklaringsbehov = repository.hentAvklaringsbehovene(behandling.id)
            assertThat(avklaringsbehov.alle()).hasSize(1)
            assertThat(avklaringsbehov.alle().get(0).erAvsluttet()).isFalse()

            avklaringsbehovene.løsAvklaringsbehov(
                definisjon = Definisjon.AVKLAR_SYKDOM,
                begrunnelse = "Godkjent",
                endretAv = "Saksbehandler",
                kreverToTrinn = true
            )

            val avklaringsbehovEtterLøst = repository.hentAvklaringsbehovene(BehandlingId(1))
            assertThat(avklaringsbehovEtterLøst.alle().get(0).erAvsluttet()).isTrue()
        }
    }

    private companion object {
        private val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))
    }

    private fun sak(connection: DBConnection): Sak {
        return sakRepository(connection).finnEllerOpprett(
            person = PersonRepository(connection).finnEllerOpprett(ident()),
            periode = periode
        )
    }

    private fun behandling(connection: DBConnection, sak: Sak): Behandling {
        val behandling = behandlingRepository(connection).finnSisteBehandlingFor(sak.id)
        if (behandling == null || behandling.status().erAvsluttet()) {
            return behandlingRepository(connection).opprettBehandling(sak.id, listOf())
        }
        return behandling
    }

}