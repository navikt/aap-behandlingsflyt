package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning

import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.komponenter.verdityper.Tid

fun validerVurderingsperioder(løsninger: List<LøsningForPeriode>) {
    val ugyldigePerioder = løsninger
        .mapNotNull {
            val tom = it.tom
            when {
                tom == null || it.fom <= tom -> null
                else -> "${it.fom}–$tom"
            }
        }
    if (ugyldigePerioder.isNotEmpty()) {
        throw UgyldigForespørselException("Ny vurderinger med ugyldige perioder: $ugyldigePerioder")
    }

    val overlapp = løsninger
        .sortedBy { it.fom }
        .windowed(2, 1)
        .mapNotNull { (vurdering, nesteVurdering) ->
            val tom = vurdering.tom ?: Tid.MAKS

            if (tom < vurdering.fom || tom >= nesteVurdering.fom)
                "${vurdering.fom}–${vurdering.tom ?: "…"} og ${nesteVurdering.fom}–${nesteVurdering.tom ?: "…"}"
            else
                null
        }
    if (overlapp.isNotEmpty()) {
        throw UgyldigForespørselException("Vurderinger har overlappende perioder: ${overlapp.joinToString()}")
    }
}
