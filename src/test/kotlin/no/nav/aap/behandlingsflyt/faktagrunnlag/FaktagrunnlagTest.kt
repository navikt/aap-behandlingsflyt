package no.nav.aap.behandlingsflyt.faktagrunnlag

import no.nav.aap.behandlingsflyt.Periode
import no.nav.aap.behandlingsflyt.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.dbstuff.DbConnection
import no.nav.aap.behandlingsflyt.dbstuff.InitTestDatabase
import no.nav.aap.behandlingsflyt.faktagrunnlag.personopplysninger.Fødselsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.personopplysninger.PersonRegisterMock
import no.nav.aap.behandlingsflyt.faktagrunnlag.personopplysninger.Personinfo
import no.nav.aap.behandlingsflyt.faktagrunnlag.yrkesskade.YrkesskadeRegisterMock
import no.nav.aap.behandlingsflyt.faktagrunnlag.yrkesskade.YrkesskadeService
import no.nav.aap.behandlingsflyt.flyt.FlytKontekst
import no.nav.aap.behandlingsflyt.sak.SakRepository
import no.nav.aap.behandlingsflyt.sak.Ident
import no.nav.aap.behandlingsflyt.sak.PersonRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class FaktagrunnlagTest {

    companion object {
        val dataSource = InitTestDatabase.dataSource
    }
    private val dbConnection = DbConnection(dataSource.connection)
    val ident = Ident("123123123123")
    val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))
    val sak =
        SakRepository(dbConnection).finnEllerOpprett(PersonRepository(dbConnection).finnEllerOpprett(ident), periode)
    val behandling =
        BehandlingRepository.finnSisteBehandlingFor(sak.id) ?: BehandlingRepository.opprettBehandling(sak.id, listOf())
    val kontekst = FlytKontekst(sak.id, behandling.id)

    @BeforeEach
    fun setUp() {
        PersonRegisterMock.konstruer(ident, Personinfo(Fødselsdato(LocalDate.now().minusYears(18))))
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
