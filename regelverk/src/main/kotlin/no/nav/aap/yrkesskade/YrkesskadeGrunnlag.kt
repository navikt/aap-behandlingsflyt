package no.nav.aap.yrkesskade

import no.nav.aap.behandling.BehandlingId

/**
 * Yrkesskade-grunnlag fra register.
 */
data class YrkesskadeGrunnlag(
    val id: Long,
    val behandlingId: BehandlingId,
    val yrkesskader: Yrkesskader,
    val oppgittYrkesskadeISøknad: Boolean?,
)