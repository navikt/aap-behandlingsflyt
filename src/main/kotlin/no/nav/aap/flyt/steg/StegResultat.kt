package no.nav.aap.flyt.steg

import no.nav.aap.domene.behandling.avklaringsbehov.Definisjon
import no.nav.aap.flyt.StegType
import no.nav.aap.flyt.kontroll.Fortsett
import no.nav.aap.flyt.kontroll.FunnetAvklaringsbehov
import no.nav.aap.flyt.kontroll.Tilbakeført
import no.nav.aap.flyt.kontroll.TilbakeførtTilAvklaringsbehov
import no.nav.aap.flyt.kontroll.Transisjon

class StegResultat(val avklaringsbehov: List<Definisjon> = listOf(),
                   val tilbakeførtTilSteg: StegType = StegType.UDEFINERT) {

    fun transisjon(): Transisjon {
        if (avklaringsbehov.isNotEmpty() && tilbakeførtTilSteg != StegType.UDEFINERT) {
            return TilbakeførtTilAvklaringsbehov(avklaringsbehov, tilbakeførtTilSteg)
        }
        if (avklaringsbehov.isNotEmpty()) {
            return FunnetAvklaringsbehov(avklaringsbehov)
        }
        if (tilbakeførtTilSteg != StegType.UDEFINERT) {
            return Tilbakeført(tilbakeførtTilSteg)
        }
        return Fortsett
    }
}
