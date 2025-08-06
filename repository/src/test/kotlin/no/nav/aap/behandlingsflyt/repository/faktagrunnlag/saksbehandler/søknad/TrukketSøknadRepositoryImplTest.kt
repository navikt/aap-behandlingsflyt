package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.søknad

import no.nav.aap.behandlingsflyt.behandling.søknad.TrukketSøknadVurdering
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.PersonRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.verdityper.dokument.JournalpostId
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.Instant

class TrukketSøknadRepositoryImplTest {
    private val dataSource = InitTestDatabase.freshDatabase()

    @Test
    fun `read and write vurdering`() {
        val behandling1 = behandling()
        val vurdering1 = TrukketSøknadVurdering(
            journalpostId = JournalpostId("111"),
            begrunnelse = "en grunn",
            vurdertAv = Bruker("Z00000"),
            vurdert = Instant.parse("2020-01-01T12:12:12Z"),
        )

        val behandling2 = behandling()
        val vurdering2 = TrukketSøknadVurdering(
            journalpostId = JournalpostId("222"),
            begrunnelse = "en annen grunn",
            vurdertAv = Bruker("Z00001"),
            vurdert = Instant.parse("2020-01-01T12:12:19Z"),
        )

        dataSource.transaction { connection ->
            val repo = TrukketSøknadRepositoryImpl(connection)
            Assertions.assertEquals(
                emptyList<TrukketSøknadVurdering>(),
                repo.hentTrukketSøknadVurderinger(behandling1)
            )
        }

        dataSource.transaction { connection ->
            val repo = TrukketSøknadRepositoryImpl(connection)
            repo.lagreTrukketSøknadVurdering(
                behandling1,
                vurdering1
            )
            Assertions.assertEquals(
                listOf(vurdering1),
                repo.hentTrukketSøknadVurderinger(behandling1)
            )
        }


        dataSource.transaction { connection ->
            val repo = TrukketSøknadRepositoryImpl(connection)

            repo.kopier(behandling1, behandling2)
            repo.lagreTrukketSøknadVurdering(behandling2, vurdering2)

            Assertions.assertEquals(
                listOf(vurdering1),
                repo.hentTrukketSøknadVurderinger(behandling1)
            )
            Assertions.assertEquals(
                listOf(vurdering1, vurdering2),
                repo.hentTrukketSøknadVurderinger(behandling2)
            )
        }
    }

    fun behandling(): BehandlingId {
        return dataSource.transaction { connection ->
            val person = PersonRepositoryImpl(connection).finnEllerOpprett(listOf(Ident("0".repeat(11))))
            val sak = SakRepositoryImpl(connection).finnEllerOpprett(person, Periode(1 januar 2025, 1 januar 2028))
            val behandling = BehandlingRepositoryImpl(connection).opprettBehandling(
                sakId = sak.id,
                vurderingsbehov = listOf(
                    VurderingsbehovMedPeriode(
                        type = no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov.MOTTATT_SØKNAD,
                        periode = Periode(1 januar 2025, 1 januar 2028),
                    )
                ),
                typeBehandling = TypeBehandling.Førstegangsbehandling,
                forrigeBehandlingId = null,
            )

            behandling.id
        }
    }
}