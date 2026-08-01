package no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt

import no.nav.aap.beregning.Månedsinntekt
import no.nav.aap.behandling.BehandlingId
import no.nav.aap.beregning.InntektPerÅr
import no.nav.aap.lookup.repository.Repository
import no.nav.aap.misc.inntekt.InntektGrunnlag

interface InntektGrunnlagRepository : Repository{
    fun lagre(
        behandlingId: BehandlingId,
        inntekter: Set<InntektPerÅr>,
        inntektPerMåned: Set<Månedsinntekt>
    )

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)
    fun hentHvisEksisterer(behandlingId: BehandlingId): InntektGrunnlag?
    fun hent(behandlingId: BehandlingId): InntektGrunnlag
}