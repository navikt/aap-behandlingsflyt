package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.meldeperiode

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.lookup.repository.Repository

interface MeldeperiodeRepository:Repository {
    fun hent(behandlingId: BehandlingId): List<Periode>
    fun hentHvisEksisterer(behandlingId: BehandlingId): List<Periode>?
    fun lagre(behandlingId: BehandlingId, meldeperioder: List<Periode>)
}