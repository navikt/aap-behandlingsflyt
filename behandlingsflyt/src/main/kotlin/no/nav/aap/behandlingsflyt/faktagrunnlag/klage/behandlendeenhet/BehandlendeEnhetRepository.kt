package no.nav.aap.behandlingsflyt.faktagrunnlag.klage.behandlendeenhet

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.lookup.repository.Repository

interface BehandlendeEnhetRepository: Repository {
    fun hentHvisEksisterer(behandlingId: BehandlingId): BehandlendeEnhetGrunnlag?
    fun lagre(behandlingId: BehandlingId, behandlendeEnhetVurdering: BehandlendeEnhetVurdering)
}