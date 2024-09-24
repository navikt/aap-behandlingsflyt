package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.flate.PeriodeDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.flate.FritakMeldepliktVurderingDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.flate.MeldepliktFritaksperiodeDto
import no.nav.aap.komponenter.type.Periode
import java.time.LocalDateTime

data class Fritaksvurdering(
    val fritaksperioder: List<Fritaksperiode>,
    val begrunnelse: String,
    val opprettetTid: LocalDateTime
) {
    init {
        require(fritaksperioder.fritaksPeriodeOverlapperIkke()) { "Ingen fritaksperioder kan overlappe" }
    }

    fun toDto(): FritakMeldepliktVurderingDto {
        return FritakMeldepliktVurderingDto(begrunnelse, opprettetTid, fritaksperioder.map(Fritaksperiode::toDto))
    }

    private fun List<Fritaksperiode>.fritaksPeriodeOverlapperIkke() = this
        .sortedBy { it.periode.fom }
        .zipWithNext()
        .none { (tidlig, sent) ->  tidlig overlapperMed sent }

    override fun equals(other: Any?): Boolean {
        return this === other || (other is Fritaksvurdering && this.valueEquals(other))
    }

    private fun valueEquals(other: Fritaksvurdering): Boolean {
        return fritaksperioder == other.fritaksperioder && begrunnelse == other.begrunnelse
    }

    override fun hashCode() = 31 * fritaksperioder.hashCode() + begrunnelse.hashCode()
}

data class Fritaksperiode(
    val periode: Periode,
    val harFritak: Boolean
) {
    fun toDto(): MeldepliktFritaksperiodeDto { return MeldepliktFritaksperiodeDto(PeriodeDto(periode), harFritak) }

    internal infix fun overlapperMed(other: Fritaksperiode): Boolean {
        return periode.overlapper(other.periode)
    }
}