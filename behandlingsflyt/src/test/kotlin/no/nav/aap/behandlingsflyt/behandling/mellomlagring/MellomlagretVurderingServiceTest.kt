package no.nav.aap.behandlingsflyt.behandling.mellomlagring

import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.help.opprettSak
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.repository.postgresRepositoryRegistry
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.tilgang.Rolle
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class MellomlagretVurderingServiceTest {

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

    private fun opprettBehandlingOgService(connection: DBConnection): Pair<Behandling, MellomlagretVurderingService> {
        val sak = opprettSak(connection, 1 januar 2025)
        val behandling = finnEllerOpprettBehandling(connection, sak)
        val repositoryProvider = postgresRepositoryRegistry.provider(connection)
        val service = MellomlagretVurderingService(repositoryProvider)
        return behandling to service
    }

    private fun lagreMellomlagretVurdering(
        connection: DBConnection,
        behandlingId: BehandlingId,
        avklaringsbehovKode: AvklaringsbehovKode
    ) {
        val repositoryProvider = postgresRepositoryRegistry.provider(connection)
        val mellomlagretVurderingRepository = repositoryProvider.provide<MellomlagretVurderingRepository>()
        mellomlagretVurderingRepository.lagre(
            MellomlagretVurdering(
                behandlingId = behandlingId,
                avklaringsbehovKode = avklaringsbehovKode,
                data = """{"test": true}""",
                vurdertAv = "A123456",
                vurdertDato = LocalDateTime.now().withNano(0)
            )
        )
    }

    @Test
    fun `skal returnere mellomlagrede vurderinger for gitt rolle før gitt steg`() {
        dataSource.transaction { connection ->
            val (behandling, service) = opprettBehandlingOgService(connection)

            // AVKLAR_STUDENT er før gitt steg men løses av SAKSBEHANDLER_NASJONAL – skal IKKE returneres
            lagreMellomlagretVurdering(connection, behandling.id, Definisjon.AVKLAR_STUDENT.kode)

            // AVKLAR_SYKDOM er før gitt steg, løses av SAKSBEHANDLER_OPPFOLGING, skal returneres
            lagreMellomlagretVurdering(connection, behandling.id, Definisjon.AVKLAR_SYKDOM.kode)

            // AVKLAR_BISTANDSBEHOV  er lik gitt steg, løses av SAKSBEHANDLER_OPPFOLGING, skal returneres
            lagreMellomlagretVurdering(connection, behandling.id,Definisjon.AVKLAR_BISTANDSBEHOV.kode)
            
            // AVKLAR_OVERGANG_UFORE er lik gitt steg, løses av SAKSBEHANDLER_OPPFOLGING, skal IKKE returneres
            lagreMellomlagretVurdering(connection, behandling.id, Definisjon.AVKLAR_OVERGANG_UFORE.kode)

            // AVKLAR_OVERGANG_ARBEID er etter gitt steg, løses av SAKSBEHANDLER_OPPFOLGING, skal IKKE returneres
            lagreMellomlagretVurdering(connection, behandling.id, Definisjon.AVKLAR_OVERGANG_ARBEID.kode)
            
            val resultat = service.hentMellomlagredeVurderingerFørSteg(
                behandling, StegType.OVERGANG_UFORE, listOf(Rolle.SAKSBEHANDLER_OPPFOLGING)
            )

            assertThat(resultat).hasSize(2)
            assertThat(resultat.map { it.avklaringsbehovKode }).containsExactlyInAnyOrder(
                Definisjon.AVKLAR_SYKDOM.kode,
                Definisjon.AVKLAR_BISTANDSBEHOV.kode
            )
        }
    }

    @Test
    fun `skal returnere tom liste når ingen mellomlagrede vurderinger finnes`() {
        dataSource.transaction { connection ->
            val (behandling, service) = opprettBehandlingOgService(connection)

            val resultat = service.hentMellomlagredeVurderingerFørSteg(
                behandling, StegType.BEKREFT_VURDERINGER_OPPFØLGING, listOf(Rolle.SAKSBEHANDLER_OPPFOLGING)
            )

            assertThat(resultat).isEmpty()
        }
    }
    
}
