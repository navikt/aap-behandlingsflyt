package no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.verdityper.Bruker
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

data class Aktivitetsplikt11_7LøsningDto(
    val begrunnelse: String,
    val erOppfylt: Boolean,
    val gjelderFra: LocalDate,
    val utfall: Utfall? = null,
    val skalIgnorereVarselFrist: Boolean = false,
) {
    fun tilVurdering(vurdertIBehandling: BehandlingId, bruker: Bruker, dato: LocalDateTime) = Aktivitetsplikt11_7Vurdering(
        begrunnelse = begrunnelse,
        erOppfylt = erOppfylt,
        utfall = utfall,
        vurdertAv = bruker.ident,
        gjelderFra = gjelderFra,
        opprettet = dato.atZone(ZoneId.of("Europe/Oslo")).toInstant(),
        vurdertIBehandling = vurdertIBehandling,
        skalIgnorereVarselFrist = skalIgnorereVarselFrist
    )
}