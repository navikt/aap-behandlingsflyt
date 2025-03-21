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
) {
    fun tilBistandVurdering(bruker: Bruker, vurderingenGjelderFra: LocalDate?) = BistandVurdering(
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

    init {
        require((erBehovForAktivBehandling || erBehovForArbeidsrettetTiltak) xor (erBehovForAnnenOppfølging != null)) {
            "erBehovForAnnenOppfølging kan bare bli besvart hvis erBehovForAktivBehandling og erBehovForArbeidsrettetTiltak er besvart med nei"
        }
        
       //  TODO: uncomment når frontend er implementert
//        require((erBehovForAktivBehandling || erBehovForArbeidsrettetTiltak || erBehovForAnnenOppfølging == true) xor (skalVurdereAapIOvergangTilUføre != null)) {
//            "skalVurdereAapIOvergangTilUføre kan bare bli besvart hvis oppfølgingsbehov er vurdert til nei"
//        }
    }
}
