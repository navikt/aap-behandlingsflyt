package no.nav.aap.behandlingsflyt.behandling.samordning

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningVurderingPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelse
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelsePeriode
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.samordning.ytelsesvurdering.SamordningYtelseVurderingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.underveis.UnderveisRepositoryImpl
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.PersonRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.test.Fakes
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Prosent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.time.LocalDate

class SamordningServiceTest {
    @Disabled("Denne produserer ingenting ennå")
    @Test
    fun `gjør vurderinger når all data er tilstede`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val ytelseVurderingRepo = SamordningYtelseVurderingRepositoryImpl(connection)
            val behandlingId = opprettSakdata(connection)
            opprettYtelseData(ytelseVurderingRepo, behandlingId)
            opprettVurderingData(ytelseVurderingRepo, behandlingId)

            val service = SamordningService(ytelseVurderingRepo, UnderveisRepositoryImpl(connection))
            assertThat(service.vurder(behandlingId)).isEmpty()
        }
    }

    @Disabled("Inntil denne gjør noe nyttig")
    @Test
    fun `kan hente og gjøre vurdering uten vurderinger`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val ytelseVurderingRepo = SamordningYtelseVurderingRepositoryImpl(connection)
            val behandlingId = opprettSakdata(connection)
            opprettYtelseData(ytelseVurderingRepo, behandlingId)

            val service = SamordningService(ytelseVurderingRepo, UnderveisRepositoryImpl(connection))
            val tidslinje = service.vurder(behandlingId)

            assertThat(tidslinje).hasSize(112)
        }
    }

    @Test
    fun `om ingen data, er svaret en tom tidslinje`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val ytelseVurderingRepo = SamordningYtelseVurderingRepositoryImpl(connection)
            val behandlingId = opprettSakdata(connection)

            val service = SamordningService(ytelseVurderingRepo, UnderveisRepositoryImpl(connection))
            val tidslinje = service.vurder(behandlingId)

            assertThat(tidslinje).isEmpty()
        }
    }

    @Test
    fun `sjekker om det er gjort vurderinger`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val ytelseVurderingRepo = SamordningYtelseVurderingRepositoryImpl(connection)
            val behandlingId = opprettSakdata(connection)
            opprettYtelseData(ytelseVurderingRepo, behandlingId)

            val service = SamordningService(ytelseVurderingRepo, UnderveisRepositoryImpl(connection))
            assertEquals(false, service.harGjortVurdering(behandlingId))

            opprettVurderingData(ytelseVurderingRepo, behandlingId)
            assertEquals(true, service.harGjortVurdering(behandlingId))
        }
    }

    private fun opprettVurderingData(repo: SamordningYtelseVurderingRepositoryImpl, behandlingId: BehandlingId) {
        repo.lagreVurderinger(
            behandlingId,
            listOf(
                SamordningVurdering(
                    Ytelse.SYKEPENGER,
                    listOf(
                        SamordningVurderingPeriode(
                            Periode(LocalDate.now(), LocalDate.now().plusDays(5)),
                            Prosent(50),
                            0
                        )
                    )
                )
            )
        )
    }

    private fun opprettYtelseData(repo: SamordningYtelseVurderingRepositoryImpl, behandlingId: BehandlingId) {
        repo.lagreYtelser(
            behandlingId,
            listOf(
                SamordningYtelse(
                    Ytelse.SYKEPENGER,
                    listOf(
                        SamordningYtelsePeriode(
                            Periode(LocalDate.now(), LocalDate.now().plusDays(5)),
                            Prosent(50),
                            0
                        )
                    ),
                    "kilde",
                    "ref"
                )
            )
        )
    }

    private fun opprettSakdata(connection: DBConnection): BehandlingId {
        val person = PersonRepositoryImpl(connection).finnEllerOpprett(listOf(Ident("ident", true)))
        val sakId = SakRepositoryImpl(connection).finnEllerOpprett(
            person,
            Periode(LocalDate.now(), LocalDate.now().plusDays(5))
        ).id
        return BehandlingRepositoryImpl(connection).opprettBehandling(
            sakId,
            listOf(),
            TypeBehandling.Førstegangsbehandling,
            null
        ).id
    }
}