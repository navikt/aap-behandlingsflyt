package no.nav.aap.flyt.kontroll

import no.nav.aap.domene.behandling.BehandlingRepository
import no.nav.aap.domene.behandling.Status
import no.nav.aap.domene.behandling.avklaringsbehov.løsning.AvklaringsbehovLøsning
import org.junit.jupiter.api.Test

class FlytKontrollerTest {

    private val flytKontroller = FlytKontroller()

    @Test
    fun name() {
        val kontekst = FlytKontekst(1L, 1L)
        flytKontroller.prosesserBehandling(kontekst)

        val behandling = BehandlingRepository.hentBehandling(kontekst.behandlingId)
        assert(behandling.status() == Status.UTREDES)
        assert(behandling.avklaringsbehov().isNotEmpty())

        flytKontroller.løsAvklaringsbehovOgFortsettProsessering(kontekst, avklaringsbehov = listOf(
            AvklaringsbehovLøsning(no.nav.aap.domene.behandling.avklaringsbehov.Definisjon.AVKLAR_YRKESSKADE, "Begrunnelse", "meg")
        ))

        assert(behandling.status() == Status.AVSLUTTET)
    }
}
