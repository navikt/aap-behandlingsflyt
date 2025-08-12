package no.nav.aap.behandlingsflyt.behandling.mellomlagring

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.repository.Repository


interface MellomlagretVurderingRepository : Repository {
    fun hentHvisEksisterer(behandlingId: BehandlingId, avklaringsbehovKode: String): MellomlagretVurdering?
    fun lagre(mellomlagretVurdering: MellomlagretVurdering)
}