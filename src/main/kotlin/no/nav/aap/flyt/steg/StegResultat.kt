package no.nav.aap.flyt.steg

import no.nav.aap.domene.behandling.avklaringsbehov.Definisjon
import no.nav.aap.flyt.StegType
import no.nav.aap.flyt.kontroll.Fortsett
import no.nav.aap.flyt.kontroll.FunnetAvklaringsbehov
import no.nav.aap.flyt.kontroll.Tilbakeført
import no.nav.aap.flyt.kontroll.TilbakeførtTilAvklaringsbehov
import no.nav.aap.flyt.kontroll.Transisjon

class StegResultat(val avklaringsbehov: List<Definisjon> = listOf(),
                   val tilbakeFørtTilSteg: StegType = StegType.UDEFINERT) {

    fun transisjon(): Transisjon {
        if (avklaringsbehov.isNotEmpty() && tilbakeFørtTilSteg != StegType.UDEFINERT) {
            return TilbakeførtTilAvklaringsbehov(avklaringsbehov, tilbakeFørtTilSteg)
        }
        if (avklaringsbehov.isNotEmpty()) {
            return FunnetAvklaringsbehov(avklaringsbehov)
        }
        if (tilbakeFørtTilSteg != StegType.UDEFINERT) {
            return Tilbakeført(tilbakeFørtTilSteg)
        }
        return Fortsett
    }
}
