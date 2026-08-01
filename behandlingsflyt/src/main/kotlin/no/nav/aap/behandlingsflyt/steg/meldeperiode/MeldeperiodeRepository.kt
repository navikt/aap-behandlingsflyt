package no.nav.aap.behandlingsflyt.steg.meldeperiode

import no.nav.aap.behandling.BehandlingId
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.lookup.repository.Repository

interface MeldeperiodeRepository : Repository {
    fun hentFørsteMeldeperiode(behandlingId: BehandlingId): Periode?
    fun hentMeldeperioder(behandlingId: BehandlingId, periode: Periode): List<Periode>
    fun lagreFørsteMeldeperiode(behandlingId: BehandlingId, meldeperiode: Periode?)
}