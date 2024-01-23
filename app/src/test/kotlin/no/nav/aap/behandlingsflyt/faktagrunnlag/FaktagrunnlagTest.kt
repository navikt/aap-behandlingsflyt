package no.nav.aap.behandlingsflyt.faktagrunnlag

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.dbconnect.transaction
import no.nav.aap.behandlingsflyt.dbtest.InitTestDatabase
import no.nav.aap.behandlingsflyt.dbtestdata.ident
import no.nav.aap.behandlingsflyt.faktagrunnlag.personopplysninger.Fødselsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.personopplysninger.Personopplysning
import no.nav.aap.behandlingsflyt.faktagrunnlag.personopplysninger.adapter.PersonRegisterMock
import no.nav.aap.behandlingsflyt.faktagrunnlag.yrkesskade.YrkesskadeService
import no.nav.aap.behandlingsflyt.faktagrunnlag.yrkesskade.adapter.YrkesskadeRegisterMock
import no.nav.aap.behandlingsflyt.flyt.FlytKontekst
import no.nav.aap.behandlingsflyt.flyt.tilKontekst
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.sakRepository
import no.nav.aap.verdityper.Periode
import no.nav.aap.verdityper.sakogbehandling.Ident
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class FaktagrunnlagTest {

    private companion object {
        private val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))
    }

    private fun klargjør(connection: DBConnection): Pair<Ident, FlytKontekst> {
        val ident = ident()
        val person = PersonRepository(connection).finnEllerOpprett(ident)
        val sak = sakRepository(connection).finnEllerOpprett(person, periode)
        val behandling = SakOgBehandlingService(connection).finnEllerOpprettBehandling(sak.saksnummer).behandling

        PersonRegisterMock.konstruer(ident, Personopplysning(Fødselsdato(LocalDate.now().minusYears(18))))

        return ident to tilKontekst(behandling)
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
}
