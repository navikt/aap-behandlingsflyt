package no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.verdityper.Bruker
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

data class Aktivitetsplikt11_9LøsningDto(
    val dato: LocalDate,
    val begrunnelse: String,
    val grunn: Grunn,
    val brudd: Brudd,
) {
    fun tilVurdering(vurdertIBehandling: BehandlingId, bruker: Bruker, opprettet: LocalDateTime) =
        Aktivitetsplikt11_9Vurdering(
            begrunnelse = begrunnelse,
            dato = dato,
            grunn = grunn,
            brudd = brudd,
            vurdertAv = bruker.ident,
            opprettet = opprettet.atZone(ZoneId.of("Europe/Oslo")).toInstant(),
            vurdertIBehandling = vurdertIBehandling,
        )
}