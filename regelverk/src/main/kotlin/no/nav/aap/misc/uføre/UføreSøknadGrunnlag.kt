package no.nav.aap.misc.uføre

import no.nav.aap.behandling.BehandlingId

data class UføreSøknadGrunnlag(
    val behandlingId: BehandlingId,
    val uføreSøknad: UføreSøknad,
)