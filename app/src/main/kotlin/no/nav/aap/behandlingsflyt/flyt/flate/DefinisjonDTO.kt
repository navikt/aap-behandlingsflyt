package no.nav.aap.behandlingsflyt.flyt.flate

import no.nav.aap.behandlingsflyt.avklaringsbehov.Definisjon
import no.nav.aap.verdityper.flyt.StegType

data class DefinisjonDTO(
    val navn: String,
    val type: String,
    val behovType: Definisjon.BehovType,
    val løsesISteg: StegType
)
