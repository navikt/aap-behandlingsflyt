package no.nav.aap.behandlingsflyt.behandling.tilkjentytelse

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.etterlevelse.sporing.SporbartResultatRepo
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.lookup.repository.Repository

interface TilkjentYtelseRepository : Repository, SporbartResultatRepo<Tidslinje<Tilkjent>> {
    fun hentHvisEksisterer(behandlingId: BehandlingId): Tidslinje<Tilkjent>?
    fun hentTilkjentUtenRedusertDagsats(behandlingId: BehandlingId): List<TilkjentForRedusertDagsatsBackfill>
    override fun lagre(behandlingId: BehandlingId, resultat: Tidslinje<Tilkjent>)
    fun backfillRedusertDagsats(tilkjentBackfillKandidater: List<TilkjentForRedusertDagsatsBackfill>)
}