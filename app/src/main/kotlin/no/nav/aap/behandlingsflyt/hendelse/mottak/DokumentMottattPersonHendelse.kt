package no.nav.aap.behandlingsflyt.hendelse.mottak

import no.nav.aap.verdityper.dokument.JournalpostId
import no.nav.aap.behandlingsflyt.hendelse.mottak.dokument.StrukturertDokument
import no.nav.aap.verdityper.Periode
import java.time.LocalDateTime

class DokumentMottattPersonHendelse(
    val journalpost: JournalpostId,
    val mottattTidspunkt: LocalDateTime,
    val strukturertDokument: StrukturertDokument<*>
) : PersonHendelse {

    override fun periode(): Periode {
        return strukturertDokument.periode()
    }

    override fun tilSakshendelse(): SakHendelse {
        return DokumentMottattSakHendelse(journalpost, mottattTidspunkt, strukturertDokument)
    }
}
