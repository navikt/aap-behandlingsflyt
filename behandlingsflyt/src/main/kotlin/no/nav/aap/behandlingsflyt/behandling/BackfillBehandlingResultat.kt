package no.nav.aap.behandlingsflyt.behandling

enum class BackfillBehandlingResultat {
    /** Behandlingen hadde allerede verdien. Resten av saken kan hoppes over. */
    AlleredeBackfilled,

    /** Verdi ble backfilled (eller det var ingenting å gjøre). */
    Backfilled,
}
