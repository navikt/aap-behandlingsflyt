package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.flate

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.BistandVurdering
import java.time.LocalDate

data class BistandVurderingDto(
    val begrunnelse: String,
    val erBehovForAktivBehandling: Boolean,
    val erBehovForArbeidsrettetTiltak: Boolean,
    val erBehovForAnnenOppfølging: Boolean?,
    val vurderingenGjelderFra: LocalDate?,
) {
    companion object {
        fun fraBistandVurdering(bistandVurdering: BistandVurdering?) = bistandVurdering?.toDto()
    }

    init {
        require((erBehovForAktivBehandling || erBehovForArbeidsrettetTiltak) xor (erBehovForAnnenOppfølging != null)) {
            "erBehovForAnnenOppfølging kan bare bli besvart hvis erBehovForAktivBehandling og erBehovForArbeidsrettetTiltak er besvart med nei"
        }
    }

    fun tilBistandVurdering() = BistandVurdering(
        begrunnelse = begrunnelse,
        erBehovForAktivBehandling = erBehovForAktivBehandling,
        erBehovForArbeidsrettetTiltak = erBehovForArbeidsrettetTiltak,
        erBehovForAnnenOppfølging = erBehovForAnnenOppfølging,
        vurderingenGjelderFra = vurderingenGjelderFra
    )
}
