package no.nav.aap.flyt.kontroll

import no.nav.aap.avklaringsbehov.yrkesskade.AvklarYrkesskadeLøsning
import no.nav.aap.domene.behandling.BehandlingTjeneste
import no.nav.aap.domene.behandling.Status
import no.nav.aap.domene.behandling.grunnlag.yrkesskade.Yrkesskade
import no.nav.aap.domene.behandling.grunnlag.yrkesskade.YrkesskadeTjeneste
import no.nav.aap.domene.behandling.grunnlag.yrkesskade.Yrkesskader
import no.nav.aap.domene.fagsak.FagsakTjeneste
import no.nav.aap.domene.typer.Ident
import no.nav.aap.domene.typer.Periode
import org.junit.jupiter.api.Test
import java.time.LocalDate

class FlytKontrollerTest {

    private val flytKontroller = FlytKontroller()

    @Test
    fun `skal avklare yrkesskade hvis det finnes spor av yrkesskade`() {
        val fagsak = FagsakTjeneste.finnEllerOpprett(Ident("123123123123"), Periode(LocalDate.now(), LocalDate.now().plusYears(3)))
        assert(fagsak.saksnummer.isNotEmpty())

        val behandling = BehandlingTjeneste.opprettBehandling(fagsak.id)

        YrkesskadeTjeneste.lagre(behandlingId = behandling.id, Yrkesskader(listOf(Yrkesskade("ASDF", Periode(LocalDate.now(), LocalDate.now())))))

        val kontekst = FlytKontekst(fagsak.id, behandling.id)
        flytKontroller.prosesserBehandling(kontekst)

        assert(behandling.avklaringsbehov().isNotEmpty())
        assert(behandling.status() == Status.UTREDES)

        flytKontroller.løsAvklaringsbehovOgFortsettProsessering(
            kontekst, avklaringsbehov = listOf(
                AvklarYrkesskadeLøsning("Begrunnelse", "meg")
            )
        )

        assert(behandling.status() == Status.AVSLUTTET)
    }

    @Test
    fun `skal IKKE avklare yrkesskade hvis det finnes spor av yrkesskade`() {
        val fagsak = FagsakTjeneste.finnEllerOpprett(Ident("123123123123"), Periode(LocalDate.now(), LocalDate.now().plusYears(3)))
        assert(fagsak.saksnummer.isNotEmpty())

        val behandling = BehandlingTjeneste.opprettBehandling(fagsak.id)

        val kontekst = FlytKontekst(fagsak.id, behandling.id)
        flytKontroller.prosesserBehandling(kontekst)

        assert(behandling.avklaringsbehov().isEmpty())
        assert(behandling.status() == Status.AVSLUTTET)
    }
}
