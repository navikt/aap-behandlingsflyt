package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.meldeperiode

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.komponenter.type.Periode

class MeldeperiodeService(
    private val meldeperiodeRepository: MeldeperiodeRepository,
) {
    fun meldeperioder(sak: Sak, behandlingId: BehandlingId): List<Periode> {
        return meldeperioder(behandlingId, sak.rettighetsperiode)
    }

    fun meldeperioder(behandlingId: BehandlingId, rettighetsperiode: Periode): List<Periode> {
        val meldeperioder = meldeperiodeRepository.hent(behandlingId)
        return meldeperioder.mapNotNull { it.overlapp(rettighetsperiode) }
    }
}