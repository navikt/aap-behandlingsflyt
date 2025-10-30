package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.søknad

import no.nav.aap.behandlingsflyt.behandling.søknad.TrukketSøknadVurdering
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.PersonRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.komponenter.dbtest.TestDataSource.Companion.invoke
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.verdityper.dokument.JournalpostId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AutoClose
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.Instant

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TrukketSøknadRepositoryImplTest {

    @AutoClose
    private val dataSource = TestDataSource()

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
            val trukketSøknadRepo = TrukketSøknadRepositoryImpl(connection)
            assertThat(trukketSøknadRepo.hentTrukketSøknadVurderinger(behandling1)).isEqualTo(emptyList<TrukketSøknadVurdering>())
        }

        dataSource.transaction { connection ->
            val trukketSøknadRepo = TrukketSøknadRepositoryImpl(connection)
            trukketSøknadRepo.lagreTrukketSøknadVurdering(
                behandling1,
                vurdering1
            )
            assertThat(trukketSøknadRepo.hentTrukketSøknadVurderinger(behandling1)).isEqualTo(listOf(vurdering1))
        }


        dataSource.transaction { connection ->
            val trukketSøknadRepo = TrukketSøknadRepositoryImpl(connection)

            trukketSøknadRepo.kopier(behandling1, behandling2)
            trukketSøknadRepo.lagreTrukketSøknadVurdering(behandling2, vurdering2)

            assertThat(trukketSøknadRepo.hentTrukketSøknadVurderinger(behandling1)).isEqualTo(listOf(vurdering1))
            assertThat(trukketSøknadRepo.hentTrukketSøknadVurderinger(behandling2)).isEqualTo(listOf(vurdering1, vurdering2))
        }
    }

    fun behandling(): BehandlingId {
        return dataSource.transaction { connection ->
            val person = PersonRepositoryImpl(connection).finnEllerOpprett(listOf(Ident("0".repeat(11))))
            val sak = SakRepositoryImpl(connection).finnEllerOpprett(person, Periode(1 januar 2025, 1 januar 2028))
            val behandling = BehandlingRepositoryImpl(connection).opprettBehandling(
                sakId = sak.id,
                typeBehandling = TypeBehandling.Førstegangsbehandling,
                forrigeBehandlingId = null,
                vurderingsbehovOgÅrsak = VurderingsbehovOgÅrsak(
                    vurderingsbehov = listOf(
                        VurderingsbehovMedPeriode(
                            type = no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov.MOTTATT_SØKNAD,
                            periode = Periode(1 januar 2025, 1 januar 2028),
                        )
                    ),
                    årsak = ÅrsakTilOpprettelse.SØKNAD,
                )
            )

            behandling.id
        }
    }
}