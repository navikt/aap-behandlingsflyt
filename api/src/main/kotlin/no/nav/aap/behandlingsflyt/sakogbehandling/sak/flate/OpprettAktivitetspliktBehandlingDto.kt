package no.nav.aap.behandlingsflyt.sakogbehandling.sak.flate

import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov

data class OpprettAktivitetspliktBehandlingDto(
    val vurderingsbehov: AktivitetspliktVurderingsbehov
)

enum class AktivitetspliktVurderingsbehov {
    AKTIVITETSPLIKT_11_7,
    AKTIVITETSPLIKT_11_9;

    fun tilVurderingsbehov(): Vurderingsbehov {
        return when (this) {
            AKTIVITETSPLIKT_11_7 -> Vurderingsbehov.AKTIVITETSPLIKT_11_7
            AKTIVITETSPLIKT_11_9 -> Vurderingsbehov.AKTIVITETSPLIKT_11_9
        }
    }
}