package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.flate

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.Fritaksvurdering
import java.time.LocalDate

data class FritaksvurderingDto(
    val harFritak: Boolean,
    val fraDato: LocalDate,
    val begrunnelse: String,
) {
    fun toFritaksvurdering() = Fritaksvurdering(
        harFritak = harFritak,
        fraDato = fraDato,
        begrunnelse = begrunnelse,
        opprettetTid = null
    )
}
