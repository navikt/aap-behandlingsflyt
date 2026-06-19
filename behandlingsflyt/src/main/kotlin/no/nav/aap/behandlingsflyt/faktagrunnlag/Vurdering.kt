package no.nav.aap.behandlingsflyt.faktagrunnlag

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.verdityper.Bruker
import java.time.Instant

interface Vurdering {
    val vurdertAv: Bruker
    val opprettet: Instant
//    val kvalitetssikret: Kvalitetssikret?
//    val besluttet: Besluttet?
    val vurdertIBehandling: BehandlingId
}

data class Kvalitetssikret(
    val kvalitetssikrer: Bruker,
    val kvalitetssikret: Instant,
)

data class Besluttet(
    val beslutter: Bruker,
    val besluttet: Instant,
)
