package no.nav.aap.behandlingsflyt.sakogbehandling.behandling

import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.lookup.repository.Repository

interface BehandlingRepository : Repository {

    fun opprettBehandling(
        sakId: SakId,
        typeBehandling: TypeBehandling,
        forrigeBehandlingId: BehandlingId?,
        vurderingsbehovOgÅrsak: VurderingsbehovOgÅrsak
    ): Behandling

    fun finnSisteBehandlingFor(sakId: SakId, behandlingstypeFilter: List<TypeBehandling>): Behandling?

    fun hentStegHistorikk(behandlingId: BehandlingId): List<StegTilstand>

    fun hentAlleFor(
        sakId: SakId,
        behandlingstypeFilter: List<TypeBehandling> = TypeBehandling.entries
    ): List<Behandling>

    fun hentAlleMedVedtakFor(
        person: Person,
        behandlingstypeFilter: List<TypeBehandling> = TypeBehandling.entries
    ): List<BehandlingMedVedtak>

    fun hent(behandlingId: BehandlingId): Behandling

    fun hent(referanse: BehandlingReferanse): Behandling

    fun hentBehandlingType(behandlingId: BehandlingId): TypeBehandling

    fun oppdaterVurderingsbehovOgÅrsak(behandling: Behandling, vurderingsbehovOgÅrsak: VurderingsbehovOgÅrsak)

    fun hentVurderingsbehovOgÅrsaker(behandlingId: BehandlingId): List<VurderingsbehovOgÅrsak>

    fun hentSakId(referanse: BehandlingReferanse): SakId

    fun oppdaterBehandlingStatus(behandlingId: BehandlingId, status: Status)

    fun leggTilNyttAktivtSteg(behandlingId: BehandlingId, tilstand: StegTilstand)

    fun flyttForrigeBehandlingId(behandlingId: BehandlingId, nyForrigeBehandlingId: BehandlingId)

    fun markerSavepoint()

    fun finnSaksnummer(referanse: BehandlingReferanse): Saksnummer
}

