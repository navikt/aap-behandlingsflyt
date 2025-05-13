package no.nav.aap.behandlingsflyt.faktagrunnlag.klage.behandlendeenhet

import java.time.Instant

data class BehandlendeEnhetVurdering(
    val skalBehandlesAvNay: Boolean,
    val skalBehandlesAvKontor: Boolean,
    val vurdertAv: String,
    val opprettet: Instant? = null
) {
    init {
        require(skalBehandlesAvNay || skalBehandlesAvKontor) {
            "Minst én av skalBehandlesAvNay eller skalBehandlesAvKontor må være true"
        }
    }
}