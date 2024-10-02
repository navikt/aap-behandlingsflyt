package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.flate.PeriodeDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.flate.FritakMeldepliktVurderingDto
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.verdityper.Tid
import java.time.LocalDate
import java.time.LocalDateTime

data class Fritaksvurdering(
    val harFritak: Boolean,
    val opprinneligFraDato: LocalDate,
    val periode: Periode?,
    val begrunnelse: String,
    val opprettetTid: LocalDateTime
) {

    constructor(harFritak: Boolean, fraDato: LocalDate, begrunnelse: String, opprettetTid: LocalDateTime): this(
        harFritak, fraDato, Periode(fraDato, Tid.MAKS), begrunnelse, opprettetTid
    )

    fun toDto(): FritakMeldepliktVurderingDto {
        return FritakMeldepliktVurderingDto(begrunnelse, opprettetTid, harFritak, periode?.let { PeriodeDto(it) }, opprinneligFraDato)
    }

    override fun equals(other: Any?): Boolean {
        return this === other || (other is Fritaksvurdering && this.valueEquals(other))
    }

    private fun valueEquals(other: Fritaksvurdering): Boolean {
        return harFritak == other.harFritak &&
                opprinneligFraDato == other.opprinneligFraDato &&
                periode == other.periode &&
                begrunnelse == other.begrunnelse
    }

    override fun hashCode(): Int {
        var result = harFritak.hashCode()
        result = 31 * result + opprinneligFraDato.hashCode()
        result = 31 * result + (periode?.hashCode() ?: 0)
        result = 31 * result + begrunnelse.hashCode()
        return result
    }
}