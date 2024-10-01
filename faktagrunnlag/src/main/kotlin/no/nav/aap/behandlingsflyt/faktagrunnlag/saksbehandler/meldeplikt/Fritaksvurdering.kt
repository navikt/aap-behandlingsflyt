package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.flate.FritakMeldepliktVurderingDto
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.verdityper.Tid
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class Fritaksvurdering(
    val harFritak: Boolean,
    val opprinneligFraDato: LocalDate,
    val periode: Periode?,
    val begrunnelse: String,
    val opprettetTid: LocalDateTime
) {
    companion object {
        private val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

        private fun localDate(date: String): LocalDate {
            return LocalDate.parse(date, dateFormatter)
        }
    }

    constructor(harFritak: Boolean, fraDato: String, begrunnelse: String, opprettetTid: LocalDateTime): this(
        harFritak, localDate(fraDato), Periode(localDate(fraDato), Tid.MAKS), begrunnelse, opprettetTid
    )

    fun toDto(): FritakMeldepliktVurderingDto {
        return FritakMeldepliktVurderingDto(begrunnelse, opprettetTid, fritaksperioder.map(Fritaksperiode::toDto))
    }

    override fun equals(other: Any?): Boolean {
        return this === other || (other is Fritaksvurdering && this.valueEquals(other))
    }

    private fun valueEquals(other: Fritaksvurdering): Boolean {
        return harFritak == other.harFritak && periode == other.periode && begrunnelse == other.begrunnelse
    }

    override fun hashCode(): Int {
        var result = harFritak.hashCode()
        result = 31 * result + periode.hashCode()
        result = 31 * result + begrunnelse.hashCode()
        return result
    }
}