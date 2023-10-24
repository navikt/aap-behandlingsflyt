package no.nav.aap.behandlingsflyt.faktagrunnlag

import no.nav.aap.behandlingsflyt.domene.Periode
import no.nav.aap.behandlingsflyt.domene.behandling.BehandlingTjeneste
import no.nav.aap.behandlingsflyt.domene.person.Ident
import no.nav.aap.behandlingsflyt.domene.person.Personlager
import no.nav.aap.behandlingsflyt.domene.sak.Sakslager
import no.nav.aap.behandlingsflyt.faktagrunnlag.yrkesskade.Yrkesskade
import no.nav.aap.behandlingsflyt.flyt.FlytKontekst
import no.nav.aap.behandlingsflyt.grunnlag.person.Fødselsdato
import no.nav.aap.behandlingsflyt.grunnlag.person.PersonRegisterMock
import no.nav.aap.behandlingsflyt.grunnlag.person.Personinfo
import no.nav.aap.behandlingsflyt.grunnlag.yrkesskade.YrkesskadeRegisterMock
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class FaktagrunnlagTest {

    val ident = Ident("123123123123")
    val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))
    val sak = Sakslager.finnEllerOpprett(Personlager.finnEllerOpprett(ident), periode)
    val behandling =
        BehandlingTjeneste.finnSisteBehandlingFor(sak.id) ?: BehandlingTjeneste.opprettBehandling(sak.id, listOf())
    val kontekst = FlytKontekst(sak.id, behandling.id)

    @BeforeEach
    fun setUp() {
        PersonRegisterMock.konstruer(ident, Personinfo(Fødselsdato(LocalDate.now().minusYears(18))))
    }

    @Test
    fun `Yrkesskadedata er oppdatert`() {
        val faktagrunnlag = Faktagrunnlag()

        faktagrunnlag.oppdaterFaktagrunnlagForKravliste(listOf(Yrkesskade()), kontekst)
        val erOppdatert = faktagrunnlag.oppdaterFaktagrunnlagForKravliste(listOf(Yrkesskade()), kontekst)

        assertThat(erOppdatert).isEmpty()
    }

    @Test
    fun `Yrkesskadedata er ikke oppdatert`() {
        val faktagrunnlag = Faktagrunnlag()

        val yrkesskade = Yrkesskade()

        YrkesskadeRegisterMock.konstruer(ident = ident, periode = periode)

        val erOppdatert = faktagrunnlag.oppdaterFaktagrunnlagForKravliste(listOf(yrkesskade), kontekst)

        assertThat(erOppdatert)
            .hasSize(1)
            .allMatch { it === yrkesskade }
    }

    @Test
    fun `Yrkesskadedata er utdatert, men har ingen endring fra registeret`() {
        val faktagrunnlag = Faktagrunnlag()

        val erOppdatert = faktagrunnlag.oppdaterFaktagrunnlagForKravliste(listOf(Yrkesskade()), kontekst)

        assertThat(erOppdatert).isEmpty()
    }
}
