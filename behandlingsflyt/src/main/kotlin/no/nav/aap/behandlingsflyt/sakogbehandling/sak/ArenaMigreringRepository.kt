package no.nav.aap.behandlingsflyt.sakogbehandling.sak

import no.nav.aap.behandlingsflyt.hendelse.datadeling.ArenaSakMedVedtakResponse

interface ArenaMigreringRepository : no.nav.aap.komponenter.repository.Repository {
    fun lagre(migrering: ArenaMigrering)
    fun hentForSakHvisEksisterer(sakId: SakId): ArenaMigrering?
    fun lagreArenaSakData(sakId: SakId, data: ArenaSakMedVedtakResponse)
}
