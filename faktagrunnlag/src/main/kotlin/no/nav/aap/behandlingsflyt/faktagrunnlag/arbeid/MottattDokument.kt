package no.nav.aap.behandlingsflyt.faktagrunnlag.arbeid

import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import no.nav.aap.verdityper.dokument.JournalpostId
import no.nav.aap.verdityper.sakogbehandling.SakId
import java.time.LocalDateTime

data class MottattDokument(
    val journalpostId: JournalpostId,
    val sakId: SakId,
    val behandlingId: BehandlingId?,
    val mottattTidspunkt: LocalDateTime,
    val type: DokumentType,
    val status: Status = Status.MOTTATT
)