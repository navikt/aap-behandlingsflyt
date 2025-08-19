package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand

import java.time.Instant
import java.time.LocalDate

class BistandVurdering(
    val begrunnelse: String,
    val erBehovForAktivBehandling: Boolean,
    val erBehovForArbeidsrettetTiltak: Boolean,
    val erBehovForAnnenOppfølging: Boolean?,
<<<<<<< HEAD
    val overgangBegrunnelse: String?,
    val skalVurdereAapIOvergangTilArbeid: Boolean?,
=======
    val skalVurdereAapIOvergangTilUføre: Boolean?,
>>>>>>> dad43f5f5 (Begynner på jobben om overgang Arbeidssøker)
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
<<<<<<< HEAD
        if (skalVurdereAapIOvergangTilArbeid != other.skalVurdereAapIOvergangTilArbeid) return false
        if (begrunnelse != other.begrunnelse) return false
=======
        if (skalVurdereAapIOvergangTilUføre != other.skalVurdereAapIOvergangTilUføre) return false
>>>>>>> dad43f5f5 (Begynner på jobben om overgang Arbeidssøker)
        if (vurdertAv != other.vurdertAv) return false
        if (vurderingenGjelderFra != other.vurderingenGjelderFra) return false

        return true
    }

    override fun hashCode(): Int {
        var result = erBehovForAktivBehandling.hashCode()
        result = 31 * result + erBehovForArbeidsrettetTiltak.hashCode()
        result = 31 * result + (erBehovForAnnenOppfølging?.hashCode() ?: 0)
<<<<<<< HEAD
        result = 31 * result + (skalVurdereAapIOvergangTilArbeid?.hashCode() ?: 0)
=======
        result = 31 * result + (skalVurdereAapIOvergangTilUføre?.hashCode() ?: 0)
>>>>>>> dad43f5f5 (Begynner på jobben om overgang Arbeidssøker)
        result = 31 * result + begrunnelse.hashCode()
        result = 31 * result + vurdertAv.hashCode()
        result = 31 * result + (vurderingenGjelderFra?.hashCode() ?: 0)
        return result
    }


}

