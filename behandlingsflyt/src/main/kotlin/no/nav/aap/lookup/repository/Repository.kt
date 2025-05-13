package no.nav.aap.lookup.repository

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId

/**
 * Marker interface for repository.
 *
 * PS: Hver gang denne implementeres, må også App.kt oppdateres for at implementasjonene
 * skal lastes i [postgresRepositoryRegistry].
 */
interface Repository: no.nav.aap.komponenter.repository.Repository {
    /** Kopier opplysninger og vurderinger fra en behandling inn i en annen.
     *
     * Denne metoden kalles når en revurdering opprettes (`tilBehandling`). Ideen er at revurderingen
     * tar utgangspunkt i de vurderingene som allerede er gjort (`fraBehandling`). I revurderingen
     * kan man så legge til nye vurderinger uten at den gamle behandlingen blir påvirket.
     **/
    fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)

    /**  Slett personopplysninger for en behandling.
     *
     * Brukes f.eks. når vi ikke lenger har behandlingsgrunnlag for å vurdere søknaden til medlemmet.
     *
     * Forventer at personopplysninger faktisk blir slettet slik at vi overholder GDPR.
     *
     * Soft-delete er ikke tilstrekkelig. Opplysningene må nulles ut eller radene må slette.
     *
     * Metoden skal kunne kalles flere ganger på en behandling (idempotent).
     **/
    fun slett(behandlingId: BehandlingId)
}