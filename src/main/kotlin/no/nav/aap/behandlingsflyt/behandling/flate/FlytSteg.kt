package no.nav.aap.behandlingsflyt.behandling.flate

import no.nav.aap.behandlingsflyt.flyt.steg.StegType

data class FlytSteg(val stegType: StegType, val avklaringsbehov: List<AvklaringsbehovDTO>, val vilkårDTO: VilkårDTO?)