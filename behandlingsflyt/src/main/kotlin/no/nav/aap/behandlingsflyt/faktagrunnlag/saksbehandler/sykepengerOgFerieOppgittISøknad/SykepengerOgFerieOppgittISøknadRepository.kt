package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykepengerOgFerieOppgittISøknad

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.lookup.repository.Repository

interface SykepengerOgFerieOppgittISøknadRepository : Repository {
    fun lagre(behandlingId: BehandlingId, sykepengerOgFerie: SykepengerOgFerieSøknad)
    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)
    fun hentHvisEksisterer(behandlingId: BehandlingId): SykepengerOgFerieSøknad?
}
