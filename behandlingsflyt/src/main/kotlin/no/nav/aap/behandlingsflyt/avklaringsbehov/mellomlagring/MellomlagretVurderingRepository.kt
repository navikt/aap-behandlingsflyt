package no.nav.aap.behandlingsflyt.avklaringsbehov.mellomlagring

import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.behandling.BehandlingId
import no.nav.aap.komponenter.repository.Repository


interface MellomlagretVurderingRepository : Repository {
    fun hentHvisEksisterer(behandlingId: BehandlingId, avklaringsbehovKode: AvklaringsbehovKode): MellomlagretVurdering?
    fun hentForAvklaringsbehov(behandlingId: BehandlingId, avklaringsbehovKoder: List<AvklaringsbehovKode>): List<MellomlagretVurdering>
    fun slett(behandlingId: BehandlingId, avklaringsbehovKode: AvklaringsbehovKode)
    fun slett(behandlingId: BehandlingId)
    fun lagre(mellomlagretVurdering: MellomlagretVurdering): MellomlagretVurdering
}