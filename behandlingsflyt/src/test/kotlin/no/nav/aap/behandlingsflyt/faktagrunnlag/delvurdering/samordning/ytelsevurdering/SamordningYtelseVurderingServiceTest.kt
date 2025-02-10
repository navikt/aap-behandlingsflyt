package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering

import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.samordning.ytelsesvurdering.SamordningYtelseVurderingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.PersonRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.behandlingsflyt.test.Fakes
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

@Fakes
class SamordningYtelseVurderingServiceTest {

    @Test
    fun `krever avklaring når endringer kommer`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val repo = SamordningYtelseVurderingRepositoryImpl(connection)
            val sakRepository = SakRepositoryImpl(connection)
            val service = SamordningYtelseVurderingService(
                SamordningYtelseVurderingRepositoryImpl(connection),
                SakService(sakRepository)
            )
            val kontekst = opprettSakdata(connection)

            //Når det ikke finnes data
            val ingenData = service.oppdater(kontekst)
            assertEquals(Informasjonskrav.Endret.ENDRET, ingenData)

            //Data er uforandret
            val sammeData = service.oppdater(kontekst)
            assertEquals(Informasjonskrav.Endret.IKKE_ENDRET, sammeData)

            //Ny data har kommet inn
            opprettYtelseData(repo, kontekst.behandlingId)
            opprettVurderingData(repo, kontekst.behandlingId)
            val nyData = service.oppdater(kontekst)
            assertEquals(Informasjonskrav.Endret.ENDRET, nyData)
        }
    }

    private fun opprettVurderingData(repo: SamordningYtelseVurderingRepositoryImpl, behandlingId: BehandlingId) {
        repo.lagreVurderinger(
            behandlingId,
            listOf(
                SamordningVurdering(
                    "myYtelse",
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
                    "myYtelse",
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

    private fun opprettSakdata(connection: DBConnection): FlytKontekstMedPerioder {
        val person = PersonRepositoryImpl(connection).finnEllerOpprett(listOf(Ident("ident", true)))
        val sakId = SakRepositoryImpl(connection).finnEllerOpprett(
            person,
            Periode(LocalDate.now(), LocalDate.now().plusDays(5))
        ).id
        val behandlingId = BehandlingRepositoryImpl(connection).opprettBehandling(
            sakId,
            listOf(),
            TypeBehandling.Førstegangsbehandling,
            null
        ).id
        return FlytKontekstMedPerioder(sakId, behandlingId, TypeBehandling.Førstegangsbehandling, setOf())
    }
}