package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.flate

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.BistandVurdering
import no.nav.aap.komponenter.httpklient.auth.Bruker
import java.time.LocalDate

data class BistandVurderingLøsningDto(
    val begrunnelse: String,
    val erBehovForAktivBehandling: Boolean,
    val erBehovForArbeidsrettetTiltak: Boolean,
    val erBehovForAnnenOppfølging: Boolean?,
    val overgangBegrunnelse: String?,
    val skalVurdereAapIOvergangTilUføre: Boolean?,
    val skalVurdereAapIOvergangTilArbeid: Boolean?,
    val vurderingenGjelderFra: LocalDate?,
) {
    fun tilBistandVurdering(bruker: Bruker) = BistandVurdering(
        begrunnelse = begrunnelse,
        erBehovForAktivBehandling = erBehovForAktivBehandling,
        erBehovForArbeidsrettetTiltak = erBehovForArbeidsrettetTiltak,
        erBehovForAnnenOppfølging = erBehovForAnnenOppfølging,
        vurderingenGjelderFra = vurderingenGjelderFra,
        overgangBegrunnelse = overgangBegrunnelse,
        skalVurdereAapIOvergangTilUføre = skalVurdereAapIOvergangTilUføre,
        skalVurdereAapIOvergangTilArbeid = skalVurdereAapIOvergangTilArbeid,
        vurdertAv = bruker.ident
    )
}
