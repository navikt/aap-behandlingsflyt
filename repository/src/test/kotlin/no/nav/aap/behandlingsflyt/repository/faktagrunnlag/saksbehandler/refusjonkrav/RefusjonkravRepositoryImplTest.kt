package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.refusjonkrav

import no.nav.aap.behandlingsflyt.behandling.avbrytrevurdering.AvbrytRevurderingVurdering
import no.nav.aap.behandlingsflyt.behandling.avbrytrevurdering.AvbrytRevurderingÅrsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.refusjonkrav.RefusjonkravVurdering
import no.nav.aap.behandlingsflyt.help.FakePdlGateway
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.avbrytrevurdering.AvbrytRevurderingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.PersonRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.test.desember
import no.nav.aap.behandlingsflyt.test.ident
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.komponenter.dbtest.TestDataSource.Companion.invoke
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Bruker
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AutoClose
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.LocalDate

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RefusjonkravRepositoryImplTest {

    @AutoClose
    private val dataSource = TestDataSource()

    @Test
    fun `lagre, hent og slett`() {
        val sak = dataSource.transaction { sak(it) }

        val behandling = dataSource.transaction {
            finnEllerOpprettBehandling(it, sak)
        }


        val periode = Periode(1 januar 2022, 31.desember(2023))
        val vurderinger = listOf(
            RefusjonkravVurdering(
                harKrav = true,
                fom = periode.fom,
                tom = periode.tom,
                vurdertAv = "saksbehandler",
                navKontor = "Nav Hamar",
            ),
            RefusjonkravVurdering(
                harKrav = true,
                fom = periode.fom,
                tom = periode.tom,
                vurdertAv = "veileder",
                navKontor = "Nav Kongsvinger",
            ),
            RefusjonkravVurdering(
                harKrav = true,
                fom = periode.fom,
                tom = periode.tom,
                vurdertAv = "kvalitetssikrer",
                navKontor = "Nav Flisa,"
            )
        )
        dataSource.transaction {
            RefusjonkravRepositoryImpl(it).lagre(
                sak.id, behandling.id, vurderinger
            )
        }

        val uthentet = dataSource.transaction {
            RefusjonkravRepositoryImpl(it).hentHvisEksisterer(behandling.id)
        }

        assertThat(uthentet).hasSameSizeAs(vurderinger)
        assertThat(uthentet).hasSize(3)
        uthentet!!.zip(vurderinger).forEach { (actual, expected) ->
            assertThat(actual.harKrav).isEqualTo(expected.harKrav)
            assertThat(actual.fom).isEqualTo(expected.fom)
            assertThat(actual.tom).isEqualTo(expected.tom)
            assertThat(actual.vurdertAv).isEqualTo(expected.vurdertAv)
        }
        // Lagre ny vurdering
        val vurderinger2 = listOf(vurderinger.first().copy(harKrav = false))
        dataSource.transaction {
            RefusjonkravRepositoryImpl(it).lagre(
                sak.id, behandling.id, vurderinger2
            )
        }
        val uthentet2 = dataSource.transaction {
            RefusjonkravRepositoryImpl(it).hentHvisEksisterer(behandling.id)
        }

        uthentet2!!.zip(vurderinger2).forEach { (actual, expected) ->
            assertThat(actual.harKrav).isEqualTo(expected.harKrav)
            assertThat(actual.fom).isEqualTo(expected.fom)
            assertThat(actual.tom).isEqualTo(expected.tom)
            assertThat(actual.vurdertAv).isEqualTo(expected.vurdertAv)
        }
        // SLETT
        dataSource.transaction {
            RefusjonkravRepositoryImpl(it).slett(behandling.id)
        }
        val uthentetEtterSletting = dataSource.transaction {
            RefusjonkravRepositoryImpl(it).hentHvisEksisterer(behandling.id)
        }
        assertThat(uthentetEtterSletting).isNull()
    }

    @Test
    fun `historikk viser kun vurderinger fra tidligere behandlinger og ikke inkluderer vurdering fra avbrutt revurdering`() {
        val refusjonkravVurdering1 = RefusjonkravVurdering(
            harKrav = true,
            fom = LocalDate.of(2023, 1, 1),
            tom = LocalDate.of(2023, 6, 20),
            vurdertAv = "Z00001",
            navKontor = "Nav Hamar",
        )

        val refusjonkravVurdering2 = RefusjonkravVurdering(
            harKrav = true,
            fom = LocalDate.of(2022, 1, 1),
            tom = LocalDate.of(2023, 10, 15),
            vurdertAv = "Z00002",
            navKontor = "Nav Oslo",
        )

        val refusjonkravVurdering3 = RefusjonkravVurdering(
            harKrav = true,
            fom = LocalDate.of(2023, 7, 1),
            tom = LocalDate.of(2023, 12, 5),
            vurdertAv = "Z00003",
            navKontor = "Nav Fredikstad",
        )

        val førstegangsbehandling = dataSource.transaction { connection ->
            val refusjonskravRepo = RefusjonkravRepositoryImpl(connection)
            val sak = sak(connection)
            val førstegangsbehandling = finnEllerOpprettBehandling(connection, sak)

            refusjonskravRepo.lagre(sak.id, førstegangsbehandling.id, listOf(refusjonkravVurdering1))
            førstegangsbehandling
        }

        dataSource.transaction { connection ->
            val refusjonskravRepo = RefusjonkravRepositoryImpl(connection)
            val avbrytRevurderingRepo = AvbrytRevurderingRepositoryImpl(connection)
            val revurderingAvbrutt = revurderingRefusjonskrav(connection, førstegangsbehandling)

            // Marker revurderingen som avbrutt
            avbrytRevurderingRepo.lagre(
                revurderingAvbrutt.id, AvbrytRevurderingVurdering(
                    AvbrytRevurderingÅrsak.REVURDERINGEN_BLE_OPPRETTET_VED_EN_FEIL, "avbryte pga. feil",
                    Bruker("Z00000")
                )
            )
            refusjonskravRepo.lagre(førstegangsbehandling.sakId, revurderingAvbrutt.id, listOf(refusjonkravVurdering2))
        }

        dataSource.transaction { connection ->
            val refusjonskravRepo = RefusjonkravRepositoryImpl(connection)
            val revurdering = revurderingRefusjonskrav(connection, førstegangsbehandling)

            refusjonskravRepo.lagre(førstegangsbehandling.sakId, revurdering.id, listOf(refusjonkravVurdering3))

            val historikk = refusjonskravRepo.hentHistoriskeVurderinger(revurdering.sakId, revurdering.id)
            assertThat(historikk).usingRecursiveComparison()
                .ignoringFields("id", "opprettetTid")
                .isEqualTo(listOf(refusjonkravVurdering1))
        }
    }

    private fun sak(connection: DBConnection): Sak {
        return PersonOgSakService(
            FakePdlGateway,
            PersonRepositoryImpl(connection),
            SakRepositoryImpl(connection)
        ).finnEllerOpprett(ident(), Periode(1 januar 2022, 31.desember(2023)))
    }

    private fun revurderingRefusjonskrav(connection: DBConnection, behandling: Behandling): Behandling {
        return BehandlingRepositoryImpl(connection).opprettBehandling(
            behandling.sakId,
            typeBehandling = TypeBehandling.Revurdering,
            forrigeBehandlingId = behandling.id,
            vurderingsbehovOgÅrsak = VurderingsbehovOgÅrsak(
                vurderingsbehov = listOf(VurderingsbehovMedPeriode(Vurderingsbehov.REFUSJONSKRAV)),
                årsak = ÅrsakTilOpprettelse.MANUELL_OPPRETTELSE
            )
        )
    }
}