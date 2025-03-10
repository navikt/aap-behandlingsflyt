package no.nav.aap.behandlingsflyt.flyt.internals

import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.StrukturertDokument
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.verdityper.dokument.JournalpostId
import java.time.LocalDateTime

data class DokumentMottattSakHendelse(
    val journalpost: JournalpostId,
    val mottattTidspunkt: LocalDateTime,
    val innsendingType: InnsendingType?,
    val strukturertDokument: StrukturertDokument<*>?
) : SakHendelse {
    
    init {
        require(innsendingType != null || strukturertDokument != null) {
            "Hvis det ikke finnes et strukturert dokument må innsendingstype være satt"
        }
    }

    override fun tilBehandlingHendelse(): BehandlingHendelse {
        return DokumentMottattBehandlingHendelse()
    }
}
