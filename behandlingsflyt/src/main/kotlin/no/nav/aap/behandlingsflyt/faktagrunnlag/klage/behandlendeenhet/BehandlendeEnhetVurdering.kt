package no.nav.aap.behandlingsflyt.faktagrunnlag.klage.behandlendeenhet

import no.nav.aap.komponenter.verdityper.Bruker
import java.time.Instant

data class BehandlendeEnhetVurdering(
    val skalBehandlesAvNay: Boolean,
    val skalBehandlesAvKontor: Boolean,
    val vurdertAv: Bruker,
    val opprettet: Instant
) {
    fun skalBehandlesAvBådeNavKontorOgNay(): Boolean = skalBehandlesAvNay && skalBehandlesAvKontor

    init {
        require(skalBehandlesAvNay || skalBehandlesAvKontor) {
            "Minst én av skalBehandlesAvNay eller skalBehandlesAvKontor må være true"
        }
    }
}