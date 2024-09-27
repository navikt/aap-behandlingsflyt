package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering

import net.bytebuddy.asm.Advice.Local
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav
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
import no.nav.aap.verdityper.flyt.FlytKontekstMedPerioder
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import no.nav.aap.verdityper.sakogbehandling.Ident
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertEquals

@Fakes
class SamordningYtelseVurderingServiceTest {

    @Test
    fun kreverAvklaringNårEndringerKommer() {
        InitTestDatabase.dataSource.transaction { connection ->
            val repo = SamordningYtelseVurderingRepository(connection)
            val service = SamordningYtelseVurderingService(connection)
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

    private fun opprettSakdata(connection: DBConnection): FlytKontekstMedPerioder {
        val person = PersonRepository(connection).finnEllerOpprett(listOf(Ident("ident", true)))
        val sakId = SakRepositoryImpl(connection).finnEllerOpprett(person, Periode(LocalDate.now(), LocalDate.now().plusDays(5))).id
        val behandlingId = BehandlingRepositoryImpl(connection).opprettBehandling(sakId, listOf(), TypeBehandling.Førstegangsbehandling).id
        return FlytKontekstMedPerioder(sakId, behandlingId, TypeBehandling.Førstegangsbehandling, setOf())
    }
}