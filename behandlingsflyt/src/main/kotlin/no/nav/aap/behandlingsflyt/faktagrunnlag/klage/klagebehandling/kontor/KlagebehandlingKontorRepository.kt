package no.nav.aap.behandlingsflyt.faktagrunnlag.klage.klagebehandling.kontor

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.lookup.repository.Repository

interface KlagebehandlingKontorRepository: Repository {
    fun hentHvisEksisterer(behandlingId: BehandlingId): KlagebehandlingKontorGrunnlag?
    fun lagre(behandlingId: BehandlingId, behandlendeEnhetVurdering: KlagevurderingKontor)
}