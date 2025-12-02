package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import java.time.Instant
import java.time.LocalDate

data class Bistandsvurdering(
    val id: Long? = null,
    val begrunnelse: String,
    val erBehovForAktivBehandling: Boolean,
    val erBehovForArbeidsrettetTiltak: Boolean,
    val erBehovForAnnenOppfølging: Boolean?,
    val overgangBegrunnelse: String?,
    val skalVurdereAapIOvergangTilArbeid: Boolean?,
    val vurdertAv: String,
    val vurderingenGjelderFra: LocalDate,
    val opprettet: Instant,
    val vurdertIBehandling: BehandlingId
) {
    fun erBehovForBistand(): Boolean {
        return (erBehovForAktivBehandling || erBehovForArbeidsrettetTiltak || erBehovForAnnenOppfølging == true)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Bistandsvurdering
        if (begrunnelse != other.begrunnelse) return false
        if (erBehovForAktivBehandling != other.erBehovForAktivBehandling) return false
        if (erBehovForArbeidsrettetTiltak != other.erBehovForArbeidsrettetTiltak) return false
        if (erBehovForAnnenOppfølging != other.erBehovForAnnenOppfølging) return false
        if (overgangBegrunnelse != other.overgangBegrunnelse) return false
        if (skalVurdereAapIOvergangTilArbeid != other.skalVurdereAapIOvergangTilArbeid) return false
        if (vurdertAv != other.vurdertAv) return false
        if (vurderingenGjelderFra != other.vurderingenGjelderFra) return false

        return true
    }

    override fun hashCode(): Int {
        var result = begrunnelse.hashCode()
        result = 31 * result + erBehovForAktivBehandling.hashCode()
        result = 31 * result + erBehovForArbeidsrettetTiltak.hashCode()
        result = 31 * result + (erBehovForAnnenOppfølging?.hashCode() ?: 0)
        result = 31 * result + (overgangBegrunnelse?.hashCode() ?: 0)
        result = 31 * result + (skalVurdereAapIOvergangTilArbeid?.hashCode() ?: 0)
        result = 31 * result + vurdertAv.hashCode()
        result = 31 * result + vurderingenGjelderFra.hashCode()
        return result
    }
}

