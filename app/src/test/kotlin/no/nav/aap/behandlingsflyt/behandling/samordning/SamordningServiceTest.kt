package no.nav.aap.behandlingsflyt.behandling.samordning

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningVurderingPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelse
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelsePeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelseVurderingRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.db.PersonRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.db.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.test.Fakes
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.verdityper.Prosent
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import no.nav.aap.verdityper.sakogbehandling.Ident
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertEquals

@Fakes
class SamordningServiceTest {

    @Test
    fun vurdererAlleRegler() {
        //TODO: Implement me når reglene eksisterer
    }

    @Test
    fun gjørVurderingerNårAllDataErTilstede() {
        InitTestDatabase.dataSource.transaction { connection ->
            val ytelseVurderingRepo = SamordningYtelseVurderingRepository(connection)
            val behandlingId = opprettSakdata(connection)
            opprettYtelseData(ytelseVurderingRepo, behandlingId)
            opprettVurderingData(ytelseVurderingRepo, behandlingId)

            val service = SamordningService(ytelseVurderingRepo, UnderveisRepository(connection))
            service.vurder(behandlingId)
        }
    }

    @Test
    fun kanHenteOgGjøreVurderingUtenVurderinger() {
        InitTestDatabase.dataSource.transaction { connection ->
            val ytelseVurderingRepo = SamordningYtelseVurderingRepository(connection)
            val behandlingId = opprettSakdata(connection)
            opprettYtelseData(ytelseVurderingRepo, behandlingId)

            val service = SamordningService(ytelseVurderingRepo, UnderveisRepository(connection))
            service.vurder(behandlingId)
        }
    }

    @Test
    fun kanGåVidereUtenNoeData() {
        InitTestDatabase.dataSource.transaction { connection ->
            val ytelseVurderingRepo = SamordningYtelseVurderingRepository(connection)
            val behandlingId = opprettSakdata(connection)

            val service = SamordningService(ytelseVurderingRepo, UnderveisRepository(connection))
            service.vurder(behandlingId)
        }
    }

    @Test
    fun sjekkerKorrektOmDetErGjortVurderinger() {
        InitTestDatabase.dataSource.transaction { connection ->
            val ytelseVurderingRepo = SamordningYtelseVurderingRepository(connection)
            val behandlingId = opprettSakdata(connection)
            opprettYtelseData(ytelseVurderingRepo, behandlingId)

            val service = SamordningService(ytelseVurderingRepo, UnderveisRepository(connection))
            assertEquals(false, service.harGjortVurdering(behandlingId))

            opprettVurderingData(ytelseVurderingRepo, behandlingId)
            assertEquals(true, service.harGjortVurdering(behandlingId))
        }
    }

    private fun opprettVurderingData(repo: SamordningYtelseVurderingRepository, behandlingId: BehandlingId) {
        repo.lagreVurderinger(
            behandlingId,
            listOf(
                SamordningVurdering(
                    "myYtelse",
                    listOf(SamordningVurderingPeriode(Periode(LocalDate.now(), LocalDate.now().plusDays(5)), Prosent(50), 0))
                )
            )
        )
    }

    private fun opprettYtelseData(repo: SamordningYtelseVurderingRepository, behandlingId: BehandlingId) {
        repo.lagreYtelser(
            behandlingId,
            listOf(SamordningYtelse(
                "myYtelse",
                listOf(SamordningYtelsePeriode(Periode(LocalDate.now(), LocalDate.now().plusDays(5)), Prosent(50), 0)),
                "kilde",
                "ref")
            )
        )
    }

    private fun opprettSakdata(connection: DBConnection): BehandlingId {
        val person = PersonRepository(connection).finnEllerOpprett(listOf(Ident("ident", true)))
        val sakId = SakRepositoryImpl(connection).finnEllerOpprett(person, Periode(LocalDate.now(), LocalDate.now().plusDays(5))).id
        return BehandlingRepositoryImpl(connection).opprettBehandling(sakId, listOf(), TypeBehandling.Førstegangsbehandling).id
    }
}