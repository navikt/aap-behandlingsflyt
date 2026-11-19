package no.nav.aap.behandlingsflyt.sakogbehandling.sak

import no.nav.aap.lookup.repository.Repository

/**
 * Kobling mellom en sak i Kelvin og saken den er migrert fra i Arena. Én rad per sak.
 *
 * [slett] sletter raden for saken bare når behandlingen er førstegangsbehandlingen, slik at f.eks. en trukket
 * klage ikke fjerner koblingen. [kopier] gjør ingenting fordi raden gjelder saken.
 */
interface ArenaMigreringRepository : Repository {
    fun lagre(migrering: ArenaMigrering)
    fun hentForSakHvisEksisterer(sakId: SakId): ArenaMigrering?
}
