package no.nav.aap.behandlingsflyt.behandling.tilkjentytelse

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.lookup.repository.Repository

interface TilkjentYtelseRepository : Repository {
    fun hentHvisEksisterer(behandlingId: BehandlingId): List<TilkjentYtelsePeriode>?
    fun hentTilkjentUtenRedusertDagsats(behandlingId: BehandlingId): List<TilkjentForRedusertDagsatsBackfill>
    fun lagre(behandlingId: BehandlingId, tilkjent: List<TilkjentYtelsePeriode>, faktagrunnlag: TilkjentYtelseGrunnlag, versjon: String)
    fun backfillRedusertDagsats(tilkjentBackfillKandidater: List<TilkjentForRedusertDagsatsBackfill>)
}