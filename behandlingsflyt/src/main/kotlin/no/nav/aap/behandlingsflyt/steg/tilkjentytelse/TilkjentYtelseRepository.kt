package no.nav.aap.behandlingsflyt.steg.tilkjentytelse

import no.nav.aap.behandling.BehandlingId
import no.nav.aap.lookup.repository.Repository
import no.nav.aap.tilkjentytelse.TilkjentYtelseGrunnlag
import no.nav.aap.tilkjentytelse.TilkjentYtelsePeriode

interface TilkjentYtelseRepository : Repository {
    fun hentHvisEksisterer(behandlingId: BehandlingId): List<TilkjentYtelsePeriode>?
    fun lagre(behandlingId: BehandlingId, tilkjent: List<TilkjentYtelsePeriode>, faktagrunnlag: TilkjentYtelseGrunnlag, versjon: String)
}