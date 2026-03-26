package no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId

data class UføreSøknadGrunnlag(
    val behandlingId: BehandlingId,
    val uføreSøknad: UføreSøknad,
)
