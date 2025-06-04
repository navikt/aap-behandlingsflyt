package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand

import java.time.Instant
import java.time.LocalDate

class BistandVurdering(
    val begrunnelse: String,
    val erBehovForAktivBehandling: Boolean,
    val erBehovForArbeidsrettetTiltak: Boolean,
    val erBehovForAnnenOppfølging: Boolean?,
    val overgangBegrunnelse: String?,
    val skalVurdereAapIOvergangTilUføre: Boolean?,
    val skalVurdereAapIOvergangTilArbeid: Boolean?,
    val vurdertAv: String,
    val vurderingenGjelderFra: LocalDate?,
    val opprettet: Instant? = null
) {
    fun erBehovForBistand(): Boolean {
        return (erBehovForAktivBehandling || erBehovForArbeidsrettetTiltak || erBehovForAnnenOppfølging == true)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BistandVurdering

        if (erBehovForAktivBehandling != other.erBehovForAktivBehandling) return false
        if (erBehovForArbeidsrettetTiltak != other.erBehovForArbeidsrettetTiltak) return false
        if (erBehovForAnnenOppfølging != other.erBehovForAnnenOppfølging) return false
        if (skalVurdereAapIOvergangTilUføre != other.skalVurdereAapIOvergangTilUføre) return false
        if (skalVurdereAapIOvergangTilArbeid != other.skalVurdereAapIOvergangTilArbeid) return false
        if (begrunnelse != other.begrunnelse) return false
        if (overgangBegrunnelse != other.overgangBegrunnelse) return false
        if (vurdertAv != other.vurdertAv) return false
        if (vurderingenGjelderFra != other.vurderingenGjelderFra) return false

        return true
    }

    override fun hashCode(): Int {
        var result = erBehovForAktivBehandling.hashCode()
        result = 31 * result + erBehovForArbeidsrettetTiltak.hashCode()
        result = 31 * result + (erBehovForAnnenOppfølging?.hashCode() ?: 0)
        result = 31 * result + (skalVurdereAapIOvergangTilUføre?.hashCode() ?: 0)
        result = 31 * result + (skalVurdereAapIOvergangTilArbeid?.hashCode() ?: 0)
        result = 31 * result + begrunnelse.hashCode()
        result = 31 * result + (overgangBegrunnelse?.hashCode() ?: 0)
        result = 31 * result + vurdertAv.hashCode()
        result = 31 * result + (vurderingenGjelderFra?.hashCode() ?: 0)
        return result
    }


}

