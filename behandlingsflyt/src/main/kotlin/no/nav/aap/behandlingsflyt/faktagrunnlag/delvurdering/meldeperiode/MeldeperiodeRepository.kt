package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.meldeperiode

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.lookup.repository.Repository
import java.time.LocalDate

interface MeldeperiodeRepository : Repository {
    fun lagreFastsattDag(behandlingId: BehandlingId, fastsattDag: LocalDate)
    fun hentFastsattDag(behandlingId: BehandlingId): LocalDate?

    fun hentMeldeperioder(
        behandlingId: BehandlingId,
        periode: Periode
    ): List<Periode> {
        val fastsattDag = hentFastsattDag(behandlingId)
        return MeldeperiodeUtleder.utledMeldeperiode(fastsattDag, periode)
    }
}