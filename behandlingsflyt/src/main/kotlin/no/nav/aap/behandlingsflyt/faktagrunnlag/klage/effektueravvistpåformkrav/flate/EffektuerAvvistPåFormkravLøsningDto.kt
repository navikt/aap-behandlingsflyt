package no.nav.aap.behandlingsflyt.faktagrunnlag.klage.effektueravvistpåformkrav.flate

import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.effektueravvistpåformkrav.EffektuerAvvistPåFormkravVurdering

data class EffektuerAvvistPåFormkravLøsningDto(
    val skalEndeligAvvises: Boolean
) {
    fun tilVurdering(): EffektuerAvvistPåFormkravVurdering {
        return EffektuerAvvistPåFormkravVurdering(
            skalEndeligAvvises = skalEndeligAvvises
        )
    }
}