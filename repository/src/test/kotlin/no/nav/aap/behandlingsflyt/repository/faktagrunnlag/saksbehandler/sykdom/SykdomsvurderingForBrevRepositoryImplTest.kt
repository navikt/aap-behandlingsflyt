package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.sykdom

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdomsvurderingbrev.SykdomsvurderingForBrev
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.repository.avklaringsbehov.FakePdlGateway
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.PersonRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.test.ident
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class SykdomsvurderingForBrevRepositoryImplTest {

    private val dataSource = InitTestDatabase.freshDatabase()

    @Test
    fun `skal lagre, hente ut og oppdatere vurdering`() {
        val behandling = dataSource.transaction { connection ->
            finnEllerOpprettBehandling(connection, sak(connection))
        }
        val vurdering = SykdomsvurderingForBrev(
            behandlingId = behandling.id,
            vurdering = "en vurdering",
            vurdertAv = "saksbehandler"
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

        dataSource.transaction { connection ->
            SykdomsvurderingForBrevRepositoryImpl(connection).lagre(behandling.id, oppdatertVurdering)
        }

        val oppdatertLagretVurdering = dataSource.transaction {
            SykdomsvurderingForBrevRepositoryImpl(it).hent(behandling.id)
        }

        assertThat(oppdatertLagretVurdering?.vurdering).isEqualTo(oppdatertVurdering.vurdering)
        assertThat(oppdatertLagretVurdering?.vurdertAv).isEqualTo(oppdatertVurdering.vurdertAv)
    }

    @Test
    fun `skal lagre og hente ut vurderinger`() {
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
            vurderingsbehov = listOf(),
            typeBehandling = TypeBehandling.Revurdering,
            forrigeBehandlingId = behandling.id,
        )
    }

    private fun sak(connection: DBConnection): Sak {
        return PersonOgSakService(
            FakePdlGateway,
            PersonRepositoryImpl(connection),
            SakRepositoryImpl(connection)
        ).finnEllerOpprett(ident(), Periode(LocalDate.now(), LocalDate.now().plusYears(1)))
    }
}