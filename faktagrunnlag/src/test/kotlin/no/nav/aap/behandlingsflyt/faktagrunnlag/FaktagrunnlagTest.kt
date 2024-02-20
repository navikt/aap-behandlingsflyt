package no.nav.aap.behandlingsflyt.faktagrunnlag

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.dbconnect.transaction
import no.nav.aap.behandlingsflyt.dbtest.InitTestDatabase
import no.nav.aap.behandlingsflyt.dbtestdata.ident
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.YrkesskadeService
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.adapter.YrkesskadeRegisterMock
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.IdentGateway
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.test.Fakes
import no.nav.aap.verdityper.Periode
import no.nav.aap.verdityper.flyt.FlytKontekst
import no.nav.aap.verdityper.sakogbehandling.Ident
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.time.LocalDate

class FaktagrunnlagTest {

    companion object {
        private val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))
        private val fakes = Fakes()

        @BeforeAll
        @JvmStatic
        internal fun beforeAll() {
        }

        @AfterAll
        @JvmStatic
        internal fun afterAll() {
            fakes.close()
        }
    }

    @Test
    fun `Yrkesskadedata er oppdatert`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val (_, kontekst) = klargjør(connection)
            val faktagrunnlag = Faktagrunnlag(connection)

            faktagrunnlag.oppdaterFaktagrunnlagForKravliste(listOf(YrkesskadeService), kontekst)
            val erOppdatert = faktagrunnlag.oppdaterFaktagrunnlagForKravliste(listOf(YrkesskadeService), kontekst)

            assertThat(erOppdatert).isEmpty()
        }
    }

    @Test
    fun `Yrkesskadedata er ikke oppdatert`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val (ident, kontekst) = klargjør(connection)
            val faktagrunnlag = Faktagrunnlag(connection)

            YrkesskadeRegisterMock.konstruer(ident = ident, periode = periode)

            val erOppdatert = faktagrunnlag.oppdaterFaktagrunnlagForKravliste(listOf(YrkesskadeService), kontekst)

            assertThat(erOppdatert)
                .hasSize(1)
                .allMatch { it === YrkesskadeService }
        }
    }

    @Test
    fun `Yrkesskadedata er utdatert, men har ingen endring fra registeret`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val (_, kontekst) = klargjør(connection)
            val faktagrunnlag = Faktagrunnlag(connection)

            val erOppdatert = faktagrunnlag.oppdaterFaktagrunnlagForKravliste(listOf(YrkesskadeService), kontekst)

            assertThat(erOppdatert).isEmpty()
        }
    }

    private fun klargjør(connection: DBConnection): Pair<Ident, FlytKontekst> {
        val ident = ident()
        val sak = PersonOgSakService(connection, FakePdlGateway).finnEllerOpprett(ident, periode)
        val behandling = SakOgBehandlingService(connection).finnEllerOpprettBehandling(sak.saksnummer).behandling

        return ident to behandling.flytKontekst()
    }
}

object FakePdlGateway : IdentGateway {
    override fun hentAlleIdenterForPerson(ident: Ident): List<Ident> {
        return listOf(ident)
    }
}