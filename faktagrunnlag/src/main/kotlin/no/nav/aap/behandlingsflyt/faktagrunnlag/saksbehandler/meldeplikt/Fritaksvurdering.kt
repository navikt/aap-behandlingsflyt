package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.flate.PeriodeDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.flate.FritakMeldepliktVurderingDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.flate.FritaksperiodeDto
import no.nav.aap.komponenter.type.Periode
import java.time.LocalDateTime

data class Fritaksvurdering(
    val fritaksperioder: List<Fritaksperiode>,
    val begrunnelse: String,
    val opprettetTid: LocalDateTime = LocalDateTime.now()
) {
    init {
        require(fritaksperioder.fritaksPeriodeOverlapperIkke()) {
            "Ingen fritaksperioder kan overlappe"
        }
    }

    fun toDto(): FritakMeldepliktVurderingDto {
        return FritakMeldepliktVurderingDto(begrunnelse, opprettetTid, fritaksperioder.map(Fritaksperiode::toDto))
    }

    private fun List<Fritaksperiode>.fritaksPeriodeOverlapperIkke() = this
        .sortedBy { it.periode.fom }
        .zipWithNext()
        .none { (tidlig, sent) ->  tidlig overlapperMed sent }
}

data class Fritaksperiode(
    val periode: Periode,
    val harFritak: Boolean
) {
    fun toDto(): FritaksperiodeDto { return FritaksperiodeDto(PeriodeDto(periode), harFritak) }

    internal infix fun overlapperMed(other: Fritaksperiode): Boolean {
        return periode.overlapper(other.periode)
    }
}