package no.nav.aap.behandlingsflyt.faktagrunnlag.klage.behandlendeenhet.flate

import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.behandlendeenhet.BehandlendeEnhetVurdering
import no.nav.aap.komponenter.httpklient.auth.Bruker

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