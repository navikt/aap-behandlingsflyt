package no.nav.aap.behandlingsflyt.faktagrunnlag.klage.klagebehandling.nay

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.lookup.repository.Repository

interface KlagebehandlingNayRepository : Repository {
    fun hentHvisEksisterer(behandlingId: BehandlingId): KlagebehandlingNayGrunnlag?
    fun lagre(behandlingId: BehandlingId, behandlendeEnhetVurdering: KlagevurderingNay)
}