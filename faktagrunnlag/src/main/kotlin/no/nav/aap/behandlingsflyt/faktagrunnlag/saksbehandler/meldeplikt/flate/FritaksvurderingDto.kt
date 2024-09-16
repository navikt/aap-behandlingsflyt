package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.flate

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.flate.PeriodeDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.FritaksPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.Fritaksvurdering

data class FritaksvurderingDto(
    val fritaksPerioder: List<FritaksPeriodeDto>,
    val begrunnelse: String,
) {
    fun toFritaksvurdering() = Fritaksvurdering(
        fritaksPerioder.map(FritaksPeriodeDto::toFritaksPeriode), begrunnelse
    )
}

data class FritaksPeriodeDto(
    val periode: PeriodeDto,
    val harFritak: Boolean
) {
    internal fun toFritaksPeriode() = FritaksPeriode(periode.toPeriode(), harFritak)
}

