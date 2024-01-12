package no.nav.aap.behandlingsflyt.flyt.flate

import no.nav.aap.behandlingsflyt.behandling.flate.AvklaringsbehovDTO
import no.nav.aap.verdityper.flyt.StegType

data class FlytSteg(val stegType: StegType, val avklaringsbehov: List<AvklaringsbehovDTO>, val vilkårDTO: VilkårDTO?)