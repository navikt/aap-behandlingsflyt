package no.nav.aap.behandlingsflyt.faktagrunnlag

import no.nav.aap.behandlingsflyt.dbstuff.DbConnection
import no.nav.aap.behandlingsflyt.dbstuff.MockConnection
import no.nav.aap.behandlingsflyt.domene.Periode
import no.nav.aap.behandlingsflyt.domene.behandling.BehandlingTjeneste
import no.nav.aap.behandlingsflyt.sak.person.Ident
import no.nav.aap.behandlingsflyt.sak.person.Personlager
import no.nav.aap.behandlingsflyt.sak.SakRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.personopplysninger.Fødselsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.personopplysninger.PersonRegisterMock
import no.nav.aap.behandlingsflyt.faktagrunnlag.personopplysninger.Personinfo
import no.nav.aap.behandlingsflyt.faktagrunnlag.yrkesskade.YrkesskadeRegisterMock
import no.nav.aap.behandlingsflyt.faktagrunnlag.yrkesskade.YrkesskadeService
import no.nav.aap.behandlingsflyt.flyt.FlytKontekst
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class FaktagrunnlagTest {

    val ident = Ident("123123123123")
    val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))
    val sak = SakRepository.finnEllerOpprett(Personlager.finnEllerOpprett(ident), periode)
    val behandling =
        BehandlingTjeneste.finnSisteBehandlingFor(sak.id) ?: BehandlingTjeneste.opprettBehandling(sak.id, listOf())
    val kontekst = FlytKontekst(sak.id, behandling.id)

    @BeforeEach
    fun setUp() {
        PersonRegisterMock.konstruer(ident, Personinfo(Fødselsdato(LocalDate.now().minusYears(18))))
    }

    private val dbConnection = DbConnection(MockConnection())

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
