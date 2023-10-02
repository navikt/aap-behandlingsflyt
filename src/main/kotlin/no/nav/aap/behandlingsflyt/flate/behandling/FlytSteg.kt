package no.nav.aap.behandlingsflyt.flate.behandling

import no.nav.aap.flyt.StegType

data class FlytSteg(val stegType: StegType, val avklaringsbehov: List<no.nav.aap.behandlingsflyt.flate.behandling.AvklaringsbehovDTO>, val vilkårDTO: VilkårDTO?) {
}