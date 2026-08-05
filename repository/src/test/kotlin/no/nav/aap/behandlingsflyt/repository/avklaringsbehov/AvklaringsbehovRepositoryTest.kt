package no.nav.aap.behandlingsflyt.repository.avklaringsbehov

import no.nav.aap.behandlingsflyt.SYSTEMBRUKER
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.help.sak
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Status
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.komponenter.verdityper.Bruker
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

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
    fun `leser og skriver tidspunkt riktig`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)
            val repository = AvklaringsbehovRepositoryImpl(connection)
            val avklaringsbehovene = Avklaringsbehovene(repository, behandling.id)
            avklaringsbehovene.leggTil(
                Definisjon.AVKLAR_SYKDOM,
                StegType.AVKLAR_SYKDOM,
                begrunnelse = "",
                bruker = SYSTEMBRUKER,
                perioderVedtaketBehøverVurdering = null,
                perioderSomIkkeErTilstrekkeligVurdert = null
            )

            val avklarSykdom= repository.hentAvklaringsbehovene(behandling.id)
                .hentBehovForDefinisjon(Definisjon.AVKLAR_SYKDOM)!!
            val endring = avklarSykdom.historikk[0]
            assertThat(endring.tidsstempel)
                .isCloseTo(LocalDateTime.now(), within(1, ChronoUnit.MINUTES))
        }
    }

    @Test
    fun `løs avklaringsbehov skal avslutte avklaringsbehovet`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)
            val repository = AvklaringsbehovRepositoryImpl(connection)
            val avklaringsbehovene = Avklaringsbehovene(repository, behandling.id)
            avklaringsbehovene.leggTil(
                Definisjon.AVKLAR_SYKDOM,
                StegType.AVKLAR_SYKDOM,
                begrunnelse = "",
                bruker = SYSTEMBRUKER,
                perioderVedtaketBehøverVurdering = null,
                perioderSomIkkeErTilstrekkeligVurdert = null
            )

            val avklaringsbehov = repository.hentAvklaringsbehovene(behandling.id)
            assertThat(avklaringsbehov.alle()).hasSize(1)
            assertThat(avklaringsbehov.alle()[0].erAvsluttet()).isFalse()

            avklaringsbehovene.løsAvklaringsbehov(
                definisjon = Definisjon.AVKLAR_SYKDOM,
                begrunnelse = "Godkjent",
                endretAv = Bruker("Saksbehandler"),
                kreverToTrinn = true
            )

            val avklaringsbehovEtterLøst = repository.hentAvklaringsbehovene(behandling.id)
            assertThat(avklaringsbehovEtterLøst.alle()[0].erAvsluttet()).isTrue()
        }
    }

    @Test
    fun `hentAlleAvklaringsbehovForSak returnerer behov for hver behandling`() {
        dataSource.transaction { connection ->
            val sak1 = sak(connection)
            val sak2 = sak(connection)
            val behandling1 = finnEllerOpprettBehandling(connection, sak1)
            val behandling2 = finnEllerOpprettBehandling(connection, sak2)
            val repository = AvklaringsbehovRepositoryImpl(connection)

            val avklaringsbehovene = Avklaringsbehovene(repository, behandling1.id)
            avklaringsbehovene.leggTil(
                Definisjon.AVKLAR_SYKDOM,
                StegType.AVKLAR_SYKDOM,
                begrunnelse = "Første behov",
                bruker = SYSTEMBRUKER,
                perioderVedtaketBehøverVurdering = null,
                perioderSomIkkeErTilstrekkeligVurdert = null
            )

            val resultat = repository.hentAlleAvklaringsbehovForSak(listOf(behandling1.id, behandling2.id))
            val resultatPerBehandling = resultat.associateBy { it.behandlingId }

            assertThat(resultat).hasSize(2)
            assertThat(resultatPerBehandling.keys).containsExactlyInAnyOrder(behandling1.id, behandling2.id)
            assertThat(resultatPerBehandling.getValue(behandling1.id).avklaringsbehov)
                .extracting("definisjon")
                .contains(Definisjon.AVKLAR_SYKDOM)
        }
    }

    @Test
    fun `opprett bruker samme avklaringsbehov ved samme definisjon`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)
            val repository = AvklaringsbehovRepositoryImpl(connection)

            repository.opprett(
                behandlingId = behandling.id,
                definisjon = Definisjon.AVKLAR_SYKDOM,
                funnetISteg = StegType.AVKLAR_SYKDOM,
                frist = null,
                begrunnelse = "Første",
                grunn = null,
                endretAv = SYSTEMBRUKER,
                perioderSomIkkeErTilstrekkeligVurdert = null,
                perioderVedtaketBehøverVurdering = null
            )
            repository.opprett(
                behandlingId = behandling.id,
                definisjon = Definisjon.AVKLAR_SYKDOM,
                funnetISteg = StegType.AVKLAR_SYKDOM,
                frist = null,
                begrunnelse = "Andre",
                grunn = null,
                endretAv = SYSTEMBRUKER,
                perioderSomIkkeErTilstrekkeligVurdert = null,
                perioderVedtaketBehøverVurdering = null
            )

            val behov = repository.hent(behandling.id)
            assertThat(behov).hasSize(1)
            assertThat(behov.first().historikk).hasSize(2)
            assertThat(behov.first().historikk.map { it.status }).containsOnly(Status.OPPRETTET)
        }
    }
}