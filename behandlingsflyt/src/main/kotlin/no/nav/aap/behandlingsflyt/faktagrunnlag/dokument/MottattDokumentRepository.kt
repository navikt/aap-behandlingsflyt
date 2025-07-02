package no.nav.aap.behandlingsflyt.faktagrunnlag.dokument

import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.DokumentRekkefølge
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Status
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.lookup.repository.Repository

interface MottattDokumentRepository : Repository {
    fun lagre(mottattDokument: MottattDokument)
    fun oppdaterStatus(
        dokumentReferanse: InnsendingReferanse,
        behandlingId: BehandlingId,
        sakId: SakId,
        status: Status
    )

    fun hentUbehandledeDokumenterAvType(sakId: SakId, dokumentType: InnsendingType): Set<MottattDokument>
    fun hentDokumentRekkefølge(sakId: SakId, type: InnsendingType): Set<DokumentRekkefølge>
    fun hentDokumenterAvType(sakId: SakId, type: InnsendingType): Set<MottattDokument>
    fun hentDokumenterAvType(behandlingId: BehandlingId, type: InnsendingType): Set<MottattDokument>
    fun hentDokumenterAvType(behandlingId: BehandlingId, typer: List<InnsendingType>): Set<MottattDokument>
}