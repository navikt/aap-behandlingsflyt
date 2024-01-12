package no.nav.aap.behandlingsflyt.hendelse.mottak

import no.nav.aap.verdityper.dokument.JournalpostId
import no.nav.aap.behandlingsflyt.hendelse.mottak.dokument.StrukturertDokument
import java.time.LocalDateTime

class DokumentMottattSakHendelse(
    val journalpost: JournalpostId,
    val mottattTidspunkt: LocalDateTime,
    val strukturertDokument: StrukturertDokument<*>
) : SakHendelse {

    override fun tilBehandlingHendelse(): BehandlingHendelse {
        return DokumentMottattBehandlingHendelse()
    }
}
