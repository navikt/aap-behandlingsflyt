package no.nav.aap.behandlingsflyt.behandling.tilkjentytelse

import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService

class TilkjentYtelseService(
    private val behandlingRepository: BehandlingRepository,
    private val tilkjentYtelseRepository: TilkjentYtelseRepository
) {
    fun hentTilkjentYtelse(behandlingReferanse: BehandlingReferanse): List<TilkjentYtelsePeriode> {
        val behandling: Behandling =
            BehandlingReferanseService(behandlingRepository).behandling(behandlingReferanse)
        val tilkjentYtelse = tilkjentYtelseRepository.hentHvisEksisterer(behandling.id) ?: return emptyList()

        return tilkjentYtelse
    }
}