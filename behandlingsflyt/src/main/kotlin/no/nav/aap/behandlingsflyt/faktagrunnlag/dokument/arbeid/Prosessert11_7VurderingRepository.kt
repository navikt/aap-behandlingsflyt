package no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.lookup.repository.Repository

interface Prosessert11_7VurderingRepository: Repository {
    fun lagre(prosessertIBehandling: BehandlingId, aktivitetspliktBehandling: BehandlingId)
    fun nyesteProsesserteAktivitetspliktBehandling(prosessertIBehandling: BehandlingId): BehandlingId?
}