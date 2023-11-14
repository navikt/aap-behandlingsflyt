package no.nav.aap.behandlingsflyt.faktagrunnlag

import no.nav.aap.behandlingsflyt.Periode
import no.nav.aap.behandlingsflyt.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.dbstuff.DBConnection
import no.nav.aap.behandlingsflyt.dbstuff.InitTestDatabase
import no.nav.aap.behandlingsflyt.faktagrunnlag.personopplysninger.Fødselsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.personopplysninger.Personopplysning
import no.nav.aap.behandlingsflyt.faktagrunnlag.personopplysninger.adapter.PersonRegisterMock
import no.nav.aap.behandlingsflyt.faktagrunnlag.yrkesskade.YrkesskadeService
import no.nav.aap.behandlingsflyt.faktagrunnlag.yrkesskade.adapter.YrkesskadeRegisterMock
import no.nav.aap.behandlingsflyt.flyt.tilKontekst
import no.nav.aap.behandlingsflyt.sak.Ident
import no.nav.aap.behandlingsflyt.sak.PersonRepository
import no.nav.aap.behandlingsflyt.sak.SakRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class FaktagrunnlagTest {

    companion object {
        val dataSource = InitTestDatabase.dataSource
    }
    private val dbConnection = DBConnection(dataSource.connection)
    val ident = Ident("123123123124")
    val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))
    val sak =
        SakRepository(dbConnection).finnEllerOpprett(PersonRepository(dbConnection).finnEllerOpprett(ident), periode)
    val behandling =
        BehandlingRepository(dbConnection).finnSisteBehandlingFor(sak.id) ?: BehandlingRepository(dbConnection).opprettBehandling(sak.id, listOf())
    val kontekst = tilKontekst(behandling)

    @BeforeEach
    fun setUp() {
        PersonRegisterMock.konstruer(ident, Personopplysning(Fødselsdato(LocalDate.now().minusYears(18))))
    }

    @Test
    fun `Yrkesskadedata er oppdatert`() {
        val faktagrunnlag = Faktagrunnlag(dbConnection)

        faktagrunnlag.oppdaterFaktagrunnlagForKravliste(listOf(YrkesskadeService()), kontekst)
        val erOppdatert = faktagrunnlag.oppdaterFaktagrunnlagForKravliste(listOf(YrkesskadeService()), kontekst)

        assertThat(erOppdatert).isEmpty()
    }

    @Test
    fun `Yrkesskadedata er ikke oppdatert`() {
        val faktagrunnlag = Faktagrunnlag(dbConnection)

        val yrkesskadeService = YrkesskadeService()

        YrkesskadeRegisterMock.konstruer(ident = ident, periode = periode)

        val erOppdatert = faktagrunnlag.oppdaterFaktagrunnlagForKravliste(listOf(yrkesskadeService), kontekst)

        assertThat(erOppdatert)
            .hasSize(1)
            .allMatch { it === yrkesskadeService }
    }

    @Test
    fun `Yrkesskadedata er utdatert, men har ingen endring fra registeret`() {
        val faktagrunnlag = Faktagrunnlag(dbConnection)

        val erOppdatert = faktagrunnlag.oppdaterFaktagrunnlagForKravliste(listOf(YrkesskadeService()), kontekst)

        assertThat(erOppdatert).isEmpty()
    }
}
