package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.flate

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.BistandVurdering
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import java.time.LocalDate

data class BistandVurderingLøsningDto(
    val begrunnelse: String,
    val vurderingenGjelderFra: LocalDate?,
    val erBehovForAktivBehandling: Boolean,
    val erBehovForArbeidsrettetTiltak: Boolean,
    val erBehovForAnnenOppfølging: Boolean?,
    val overgangBegrunnelse: String?,
    val skalVurdereAapIOvergangTilUføre: Boolean?,
    val skalVurdereAapIOvergangTilArbeid: Boolean?,
) {
    fun tilBistandVurdering(bruker: Bruker) = BistandVurdering(
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

    fun valider(gjeldende: Tidslinje<BistandVurdering>, rettighetsperiode: Periode) {
        val gyldigAnnenOppfølging =
            (erBehovForAktivBehandling || erBehovForArbeidsrettetTiltak) xor (erBehovForAnnenOppfølging != null)
        if (!gyldigAnnenOppfølging) throw UgyldigForespørselException(
            "erBehovForAnnenOppfølging kan bare bli besvart hvis erBehovForAktivBehandling og erBehovForArbeidsrettetTiltak er besvart med nei"
        )
        if (!gjeldende.helePerioden().inneholder(rettighetsperiode)) {
            throw UgyldigForespørselException("Bistandvurdering må dekke hele rettighetsperioden")
        }
        if (!gjeldende.erSammenhengende()) {
            throw UgyldigForespørselException("Det mangler bistandvurderinger i noen perioder")
        }
    }
}
