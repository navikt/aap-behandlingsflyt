package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.samid

import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId

data class HentSamId(
    val behandlingId: BehandlingId
)

data class SamId(
    val id: String
)