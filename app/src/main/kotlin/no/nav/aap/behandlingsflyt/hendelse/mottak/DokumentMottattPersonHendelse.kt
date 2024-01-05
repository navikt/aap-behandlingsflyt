package no.nav.aap.behandlingsflyt.hendelse.mottak

import no.nav.aap.behandlingsflyt.verdityper.Periode
import no.nav.aap.behandlingsflyt.behandling.dokumenter.JournalpostId
import no.nav.aap.behandlingsflyt.hendelse.mottak.dokument.StrukturertDokument
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
