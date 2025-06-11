package no.nav.aap.behandlingsflyt.flyt.internals

import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.StrukturertDokument
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Melding
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.verdityper.dokument.JournalpostId
import java.time.LocalDateTime

class DokumentMottattPersonHendelse(
    val journalpost: JournalpostId,
    val mottattTidspunkt: LocalDateTime,
    val innsendingType: InnsendingType? = null,
    val strukturertDokument: StrukturertDokument<Melding>?,
    val periode: Periode
) : PersonHendelse {

    init {
        require(innsendingType != null || strukturertDokument != null) {
            "Hvis det ikke finnes et strukturert dokument må innsendingstype være satt"
        }
    }

    override fun periode(): Periode {
        return periode
    }

    override fun tilSakshendelse(): SakHendelse {
        return DokumentMottattSakHendelse(journalpost, mottattTidspunkt, innsendingType, strukturertDokument)
    }
}
