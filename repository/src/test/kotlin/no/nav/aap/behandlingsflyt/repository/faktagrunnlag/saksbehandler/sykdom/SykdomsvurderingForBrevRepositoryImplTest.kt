package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.sykdom

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdomsvurderingbrev.SykdomsvurderingForBrev
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.help.sak
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

internal class SykdomsvurderingForBrevRepositoryImplTest {
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
    fun `skal lagre og hente ut vurdering`() {
        val behandling = dataSource.transaction { connection ->
            finnEllerOpprettBehandling(connection, sak(connection))
        }

        val vurdering = SykdomsvurderingForBrev(
            behandlingId = behandling.id,
            vurdering = "en vurdering",
            vurdertAv = "saksbehandler",
            vurdertTidspunkt = LocalDateTime.now(),
        )

        dataSource.transaction { connection ->
            SykdomsvurderingForBrevRepositoryImpl(connection).lagre(behandling.id, vurdering)
        }

        val lagretVurdering = dataSource.transaction {
            SykdomsvurderingForBrevRepositoryImpl(it).hent(behandling.id)
        }

        assertThat(lagretVurdering?.vurdering).isEqualTo(vurdering.vurdering)
        assertThat(lagretVurdering?.vurdertAv).isEqualTo(vurdering.vurdertAv)
        assertThat(lagretVurdering?.behandlingId).isEqualTo(vurdering.behandlingId)
        assertThat(lagretVurdering?.vurdertTidspunkt).isNotNull

        val oppdatertVurdering = vurdering.copy(
            vurdering = "oppdatert vurdering",
            vurdertAv = "annen saksbehandler"
        )

        val oppdatertLagretVurdering = dataSource.transaction { connection ->
            SykdomsvurderingForBrevRepositoryImpl(connection).lagre(behandling.id, oppdatertVurdering)
            SykdomsvurderingForBrevRepositoryImpl(connection).hent(behandling.id)!!
        }

        assertThat(oppdatertLagretVurdering.vurdering).isEqualTo(oppdatertVurdering.vurdering)
        assertThat(oppdatertLagretVurdering.vurdertAv).isEqualTo(oppdatertVurdering.vurdertAv)
    }

    @Test
    fun `skal oppdatere vurdering`() {
        val behandling = dataSource.transaction { connection ->
            finnEllerOpprettBehandling(connection, sak(connection))
        }

        val vurdering = SykdomsvurderingForBrev(
            behandlingId = behandling.id,
            vurdering = "en vurdering",
            vurdertAv = "saksbehandler",
            vurdertTidspunkt = LocalDateTime.now(),
        )

        dataSource.transaction { connection ->
            SykdomsvurderingForBrevRepositoryImpl(connection).lagre(behandling.id, vurdering)
        }

        val oppdatertVurdering = vurdering.copy(
            vurdering = "oppdatert vurdering",
            vurdertAv = "annen saksbehandler",
            vurdertTidspunkt = LocalDateTime.now(),
        )

        val oppdatertLagretVurdering = dataSource.transaction { connection ->
            SykdomsvurderingForBrevRepositoryImpl(connection).lagre(behandling.id, oppdatertVurdering)
            SykdomsvurderingForBrevRepositoryImpl(connection).hent(behandling.id)!!
        }

        assertThat(oppdatertLagretVurdering.vurdering).isEqualTo(oppdatertVurdering.vurdering)
        assertThat(oppdatertLagretVurdering.vurdertAv).isEqualTo(oppdatertVurdering.vurdertAv)
    }

    @Test
    fun `skal hente ut vurderinger for sak`() {
        val sak = dataSource.transaction { connection -> sak(connection) }
        val behandling = dataSource.transaction { connection ->
            finnEllerOpprettBehandling(connection, sak)
        }

        val vurdering = SykdomsvurderingForBrev(
            behandlingId = behandling.id,
            vurdering = "en vurdering",
            vurdertAv = "saksbehandler"
        )

        dataSource.transaction { connection ->
            SykdomsvurderingForBrevRepositoryImpl(connection).lagre(behandling.id, vurdering)
        }

        val revurdering = dataSource.transaction { connection ->
            revurdering(connection, behandling)
        }

        val nyVurdering = SykdomsvurderingForBrev(
            behandlingId = revurdering.id,
            vurdering = "en ny vurdering",
            vurdertAv = "saksbehandler"
        )

        dataSource.transaction { connection ->
            SykdomsvurderingForBrevRepositoryImpl(connection).lagre(revurdering.id, nyVurdering)
        }

        val vurderingerForSak = dataSource.transaction {
            SykdomsvurderingForBrevRepositoryImpl(it).hent(sak.id)
        }

        assertThat(vurderingerForSak).size().isEqualTo(2)
    }

    private fun revurdering(connection: DBConnection, behandling: Behandling): Behandling {
        return BehandlingRepositoryImpl(connection).opprettBehandling(
            behandling.sakId,
            typeBehandling = TypeBehandling.Revurdering,
            forrigeBehandlingId = behandling.id,
            vurderingsbehovOgÅrsak = VurderingsbehovOgÅrsak(
                vurderingsbehov = listOf(VurderingsbehovMedPeriode(Vurderingsbehov.MOTTATT_SØKNAD)),
                årsak = ÅrsakTilOpprettelse.SØKNAD
            )
        )
    }
}