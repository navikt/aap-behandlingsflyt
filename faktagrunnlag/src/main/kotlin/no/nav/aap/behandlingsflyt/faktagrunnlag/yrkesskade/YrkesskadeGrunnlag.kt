package no.nav.aap.behandlingsflyt.faktagrunnlag.yrkesskade

import no.nav.aap.verdityper.sakogbehandling.BehandlingId

class YrkesskadeGrunnlag(val id: Long, val behandlingId: BehandlingId, val yrkesskader: Yrkesskader){
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as YrkesskadeGrunnlag

        if (behandlingId != other.behandlingId) return false
        if (yrkesskader != other.yrkesskader) return false

        return true
    }

    override fun hashCode(): Int {
        var result = behandlingId.hashCode()
        result = 31 * result + yrkesskader.hashCode()
        return result
    }
}
