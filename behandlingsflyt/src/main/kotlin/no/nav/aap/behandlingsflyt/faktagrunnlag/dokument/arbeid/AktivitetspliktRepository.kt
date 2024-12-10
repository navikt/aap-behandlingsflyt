package no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid

import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.lookup.repository.Repository

interface AktivitetspliktRepository : Repository {
    fun lagreBrudd(sakId: SakId, brudd: List<DokumentInput>): InnsendingId
    fun nyttGrunnlag(behandlingId: BehandlingId, brudd: Set<AktivitetspliktDokument>)
    fun hentGrunnlagHvisEksisterer(behandlingId: BehandlingId): AktivitetspliktGrunnlag?
    fun hentBrudd(sakId: SakId): List<AktivitetspliktDokument>
    fun hentBruddForInnsending(innsendingId: InnsendingId): List<AktivitetspliktDokument>
    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)
}