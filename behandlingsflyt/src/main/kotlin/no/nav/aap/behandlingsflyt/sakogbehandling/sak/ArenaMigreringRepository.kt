package no.nav.aap.behandlingsflyt.sakogbehandling.sak

interface ArenaMigreringRepository : no.nav.aap.komponenter.repository.Repository {
    fun lagre(migrering: ArenaMigrering)
    fun hentForSakHvisEksisterer(sakId: SakId): ArenaMigrering?
}
