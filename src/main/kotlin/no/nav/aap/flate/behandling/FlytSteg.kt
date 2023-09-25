package no.nav.aap.flate.behandling

import no.nav.aap.flyt.StegType

data class FlytSteg(val stegType: StegType, val avklaringsbehov: List<AvklaringsbehovDTO>, val vilkårDTO: VilkårDTO?) {
}