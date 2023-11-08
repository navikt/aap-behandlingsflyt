package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.behandling.BehandlingType

data class BehandlingSkrivelås(val id: BehandlingId, val versjon: Long, val behandlingType: BehandlingType)
