package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.flate

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.BistandVurdering
import java.time.LocalDate

data class BistandVurderingDto(
    val begrunnelse: String,
    val erBehovForAktivBehandling: Boolean,
    val erBehovForArbeidsrettetTiltak: Boolean,
    val erBehovForAnnenOppfølging: Boolean?,
    val overgangBegrunnelse: String?,
    val skalVurdereAapIOvergangTilUføre: Boolean?,
    val skalVurdereAapIOvergangTilArbeid: Boolean?,
    val vurderingenGjelderFra: LocalDate?,

    val vurdertDato: LocalDate?,
    val vurdertAv: String,
) {
    companion object {
        fun fraBistandVurdering(bistandVurdering: BistandVurdering?) = bistandVurdering?.toDto()
    }
}
