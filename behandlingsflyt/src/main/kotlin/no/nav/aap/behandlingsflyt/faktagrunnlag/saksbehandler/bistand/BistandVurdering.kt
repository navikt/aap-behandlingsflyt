package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.flate.BistandVurderingDto
import java.time.LocalDate

data class BistandVurdering(
    val begrunnelse: String,
    val erBehovForAktivBehandling: Boolean,
    val erBehovForArbeidsrettetTiltak: Boolean,
    val erBehovForAnnenOppfølging: Boolean?,
    val vurderingenGjelderFra: LocalDate?
) {
    fun toDto() = BistandVurderingDto(
        begrunnelse = begrunnelse,
        erBehovForAktivBehandling = erBehovForAktivBehandling,
        erBehovForArbeidsrettetTiltak = erBehovForArbeidsrettetTiltak,
        erBehovForAnnenOppfølging = erBehovForAnnenOppfølging,
        vurderingenGjelderFra = vurderingenGjelderFra
    )

    fun erBehovForBistand(): Boolean {
        return (erBehovForAktivBehandling || erBehovForArbeidsrettetTiltak || erBehovForAnnenOppfølging == true)
    }

}

