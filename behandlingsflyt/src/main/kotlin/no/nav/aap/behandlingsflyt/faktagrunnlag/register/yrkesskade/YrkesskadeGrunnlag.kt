package no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId

/**
 * Yrkesskade-grunnlag fra register.
 */
data class YrkesskadeGrunnlag(val id: Long, val behandlingId: BehandlingId, val yrkesskader: Yrkesskader)
