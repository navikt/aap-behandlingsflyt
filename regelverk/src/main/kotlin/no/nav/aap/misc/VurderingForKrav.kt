package no.nav.aap.misc

import java.time.Instant
import no.nav.aap.krav.Kravreferanse
import no.nav.aap.behandling.BehandlingId
import no.nav.aap.komponenter.verdityper.Bruker

interface VurderingForKrav {
    val referanse: Kravreferanse
    val vurdertIBehandling: BehandlingId
    val vurdertAv: Bruker
    val opprettet: Instant
}