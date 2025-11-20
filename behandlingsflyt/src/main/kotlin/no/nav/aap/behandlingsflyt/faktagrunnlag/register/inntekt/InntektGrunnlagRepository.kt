package no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt

import no.nav.aap.behandlingsflyt.behandling.beregning.InntektsPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.lookup.repository.Repository

interface InntektGrunnlagRepository : Repository{
    fun lagre(
        behandlingId: BehandlingId,
        inntekter: Set<InntektPerÅr>,
        inntektPerMåned: Set<InntektsPeriode>
    )

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)
    fun hentHvisEksisterer(behandlingId: BehandlingId): InntektGrunnlag?
    fun hent(behandlingId: BehandlingId): InntektGrunnlag
}