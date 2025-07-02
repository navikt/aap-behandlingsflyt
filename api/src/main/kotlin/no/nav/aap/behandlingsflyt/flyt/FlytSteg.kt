package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType

data class FlytSteg(val stegType: StegType, val avklaringsbehov: List<AvklaringsbehovDTO>, val vilkårDTO: VilkårDTO?)