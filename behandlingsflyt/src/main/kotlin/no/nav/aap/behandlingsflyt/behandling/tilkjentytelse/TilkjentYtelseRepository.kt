package no.nav.aap.behandlingsflyt.behandling.tilkjentytelse

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.lookup.repository.Repository

interface TilkjentYtelseRepository : Repository {
    fun hentHvisEksisterer(behandlingId: BehandlingId): Tidslinje<Tilkjent>?
    fun lagre(behandlingId: BehandlingId, tilkjent: Tidslinje<Tilkjent>)
}