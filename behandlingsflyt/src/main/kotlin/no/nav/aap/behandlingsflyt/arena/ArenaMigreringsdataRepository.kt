package no.nav.aap.behandlingsflyt.arena

import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.repository.Repository
import java.time.Instant

/**
 * Logg over data hentet fra Arena ved migrering. Inneholder helseopplysninger og skal aldri logges.
 */
interface ArenaMigreringsdataRepository : Repository {
    /**
     * Lagrer [data] som ny aktiv rad for [behandlingId] og [steg], og deaktiverer forrige.
     * Gjør ingenting hvis aktiv rad allerede har samme data, slik at [hentetTidspunkt]
     * viser første gang dataene ble sett. `null` lagres som SQL NULL og betyr at Arena
     * svarte uten data, f.eks. at det ikke finnes refusjonskrav.
     */
    fun lagre(behandlingId: BehandlingId, steg: StegType, data: Any?, hentetTidspunkt: Instant)
    fun hentAktivHvisEksisterer(behandlingId: BehandlingId, steg: StegType): ArenaMigreringsdata?
}

data class ArenaMigreringsdata(
    val behandlingId: BehandlingId,
    val steg: StegType,
    val hentetTidspunkt: Instant,
    val data: String?,
)
