package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangufore

import java.time.Instant
import java.time.LocalDate

class OvergangUføreVurdering(
    val begrunnelse: String,
    val brukerSoktUforetrygd: Boolean,
    val brukerVedtakUforetrygd: String?,
    val brukerRettPaaAAP: Boolean?,
    val virkningsDato: LocalDate?,
    val vurdertAv: String,
    val vurderingenGjelderFra: LocalDate?,
    val opprettet: Instant? = null
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as OvergangUføreVurdering

        if (brukerSoktUforetrygd != other.brukerSoktUforetrygd) return false
        if (brukerVedtakUforetrygd != other.brukerVedtakUforetrygd) return false
        if (brukerRettPaaAAP != other.brukerRettPaaAAP) return false
        if (virkningsDato != other.virkningsDato) return false
        if (begrunnelse != other.begrunnelse) return false
        if (vurdertAv != other.vurdertAv) return false
        if (vurderingenGjelderFra != other.vurderingenGjelderFra) return false

        return true
    }

    override fun hashCode(): Int {
        var result = brukerSoktUforetrygd.hashCode()
        result = 31 * result + brukerVedtakUforetrygd.hashCode()
        result = 31 * result + (brukerRettPaaAAP?.hashCode() ?: 0)
        result = 31 * result + (virkningsDato?.hashCode() ?: 0)
        result = 31 * result + begrunnelse.hashCode()
        result = 31 * result + vurdertAv.hashCode()
        result = 31 * result + (vurderingenGjelderFra?.hashCode() ?: 0)
        return result
    }


}

