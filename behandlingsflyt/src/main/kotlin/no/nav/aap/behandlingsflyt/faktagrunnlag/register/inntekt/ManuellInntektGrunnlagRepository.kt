package no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt

import no.nav.aap.beregning.ManuellInntektVurdering
import no.nav.aap.behandling.BehandlingId
import no.nav.aap.lookup.repository.Repository
import no.nav.aap.misc.inntekt.ManuellInntektGrunnlag

interface ManuellInntektGrunnlagRepository : Repository {
    fun lagre(behandlingId: BehandlingId, manuellVurderinger: Set<ManuellInntektVurdering>)
    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)
    fun hentHvisEksisterer(behandlingId: BehandlingId): ManuellInntektGrunnlag?
}