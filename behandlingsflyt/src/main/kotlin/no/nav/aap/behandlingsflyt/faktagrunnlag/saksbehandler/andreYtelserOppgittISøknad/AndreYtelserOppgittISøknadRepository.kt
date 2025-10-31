package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.andreYtelserOppgittISøknad

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.AndreYtelserSøknad
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.lookup.repository.Repository

interface AndreYtelserOppgittISøknadRepository : Repository {
    fun lagre(behandlingId: BehandlingId, andreUtbetalinger: AndreYtelserSøknad)
    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)
    fun hentHvisEksisterer(behandlingId: BehandlingId): AndreYtelserSøknad?
    fun hent(behandlingId: BehandlingId): AndreYtelserSøknad
}