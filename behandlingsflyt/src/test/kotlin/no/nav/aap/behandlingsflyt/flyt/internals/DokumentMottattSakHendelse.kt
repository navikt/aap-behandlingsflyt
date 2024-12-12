package no.nav.aap.behandlingsflyt.flyt.internals

import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.StrukturertDokument
import no.nav.aap.verdityper.dokument.JournalpostId
import java.time.LocalDateTime

data class DokumentMottattSakHendelse(
    val journalpost: JournalpostId,
    val mottattTidspunkt: LocalDateTime,
    val strukturertDokument: StrukturertDokument<*>
) : SakHendelse {

    override fun tilBehandlingHendelse(): BehandlingHendelse {
        return DokumentMottattBehandlingHendelse()
    }
}
