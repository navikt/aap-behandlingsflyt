package no.nav.aap.behandlingsflyt.flyt.internals

import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.StrukturertDokument
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.verdityper.dokument.JournalpostId
import java.time.LocalDateTime

class DokumentMottattPersonHendelse(
    val journalpost: JournalpostId,
    val mottattTidspunkt: LocalDateTime,
    val strukturertDokument: StrukturertDokument<*>,
    val periode: Periode
) : PersonHendelse {

    override fun periode(): Periode {
        return periode
    }

    override fun tilSakshendelse(): SakHendelse {
        return DokumentMottattSakHendelse(journalpost, mottattTidspunkt, strukturertDokument)
    }
}
