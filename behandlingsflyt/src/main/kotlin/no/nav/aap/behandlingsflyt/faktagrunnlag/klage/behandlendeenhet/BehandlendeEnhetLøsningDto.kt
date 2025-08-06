package no.nav.aap.behandlingsflyt.faktagrunnlag.klage.behandlendeenhet

import no.nav.aap.komponenter.verdityper.Bruker

data class BehandlendeEnhetLøsningDto(
    val skalBehandlesAvNay: Boolean,
    val skalBehandlesAvKontor: Boolean
) {
    fun tilVurdering(bruker: Bruker) = BehandlendeEnhetVurdering(
        skalBehandlesAvNay = skalBehandlesAvNay,
        skalBehandlesAvKontor = skalBehandlesAvKontor,
        vurdertAv = bruker.ident
    )
}