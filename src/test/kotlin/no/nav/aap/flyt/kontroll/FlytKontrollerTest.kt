package no.nav.aap.flyt.kontroll

import no.nav.aap.avklaringsbehov.yrkesskade.AvklarYrkesskadeLøsning
import no.nav.aap.domene.behandling.BehandlingTjeneste
import no.nav.aap.domene.behandling.Status
import no.nav.aap.domene.fagsak.FagsakTjeneste
import no.nav.aap.domene.typer.Ident
import no.nav.aap.domene.typer.Periode
import org.junit.jupiter.api.Test
import java.time.LocalDate

class FlytKontrollerTest {

    private val flytKontroller = FlytKontroller()

    @Test
    fun name() {
        val fagsak = FagsakTjeneste.finnEllerOpprett(Ident("123123123123"), Periode(LocalDate.now(), LocalDate.now().plusYears(3)))
        val behandling = BehandlingTjeneste.opprettBehandling(fagsak.id)
        val kontekst = FlytKontekst(fagsak.id, behandling.id)
        flytKontroller.prosesserBehandling(kontekst)

        assert(behandling.status() == Status.UTREDES)
        assert(behandling.avklaringsbehov().isNotEmpty())

        flytKontroller.løsAvklaringsbehovOgFortsettProsessering(
            kontekst, avklaringsbehov = listOf(
                AvklarYrkesskadeLøsning("Begrunnelse", "meg")
            )
        )

        assert(behandling.status() == Status.AVSLUTTET)
    }
}
