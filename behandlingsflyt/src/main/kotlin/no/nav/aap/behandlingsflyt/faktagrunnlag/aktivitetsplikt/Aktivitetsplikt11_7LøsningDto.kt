package no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt

import no.nav.aap.komponenter.verdityper.Bruker
import java.time.LocalDateTime
import java.time.ZoneId

data class Aktivitetsplikt11_7LøsningDto(
    val begrunnelse: String,
    val erOppfylt: Boolean,
    val utfall: Utfall? = null
) {
    fun tilVurdering(bruker: Bruker, dato: LocalDateTime) = Aktivitetsplikt11_7Vurdering(
        begrunnelse = begrunnelse,
        erOppfylt = erOppfylt,
        utfall = utfall,
        vurdertAv = bruker.ident,
        opprettet = dato.atZone(ZoneId.of("Europe/Oslo")).toInstant(),
    )
}