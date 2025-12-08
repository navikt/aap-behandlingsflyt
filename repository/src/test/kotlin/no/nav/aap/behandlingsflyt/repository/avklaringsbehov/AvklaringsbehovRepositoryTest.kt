package no.nav.aap.behandlingsflyt.repository.avklaringsbehov

import no.nav.aap.behandlingsflyt.SYSTEMBRUKER
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.help.sak
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class AvklaringsbehovRepositoryTest {
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
    fun `løs avklaringsbehov skal avslutte avklaringsbehovet`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)
            val repository = AvklaringsbehovRepositoryImpl(connection)
            val avklaringsbehovene = Avklaringsbehovene(repository, behandling.id)
            avklaringsbehovene.leggTil(
                listOf(Definisjon.AVKLAR_SYKDOM), StegType.AVKLAR_SYKDOM, begrunnelse = "", bruker = SYSTEMBRUKER, perioderVedtaketBehøverVurdering = null, perioderSomIkkeErTilstrekkeligVurdert =  null
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
}