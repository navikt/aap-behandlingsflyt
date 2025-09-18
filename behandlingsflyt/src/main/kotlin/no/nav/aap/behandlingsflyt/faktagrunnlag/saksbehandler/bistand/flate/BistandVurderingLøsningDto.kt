package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.flate

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.BistandVurdering
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.komponenter.miljo.Miljø
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
        skalVurdereAapIOvergangTilUføre = skalVurdereAapIOvergangTilUføre,
        overgangBegrunnelse = overgangBegrunnelse,
        skalVurdereAapIOvergangTilArbeid = skalVurdereAapIOvergangTilArbeid,
        vurdertAv = bruker.ident
    )

    fun valider() {
        if (Miljø.erProd() || Miljø.erDev() ||  Miljø.erLokal()) {
            val gyldigAnnenOppfølging =
                (erBehovForAktivBehandling || erBehovForArbeidsrettetTiltak) xor (erBehovForAnnenOppfølging != null)
            if (!gyldigAnnenOppfølging) throw UgyldigForespørselException(
                "erBehovForAnnenOppfølging kan bare bli besvart hvis erBehovForAktivBehandling og erBehovForArbeidsrettetTiltak er besvart med nei"
            )
        }
    }
}
