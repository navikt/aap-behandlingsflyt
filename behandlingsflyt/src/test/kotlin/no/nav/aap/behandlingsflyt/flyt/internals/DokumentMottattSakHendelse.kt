package no.nav.aap.behandlingsflyt.flyt.internals

import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.StrukturertDokument
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Melding
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.innsendingType
import no.nav.aap.verdityper.dokument.JournalpostId
import java.time.LocalDateTime

data class DokumentMottattSakHendelse(
    val journalpost: JournalpostId,
    val mottattTidspunkt: LocalDateTime,
    private val innsendingType: InnsendingType?,
    val strukturertDokument: StrukturertDokument<Melding>?
) : SakHendelse {
    
    init {
        require(innsendingType != null || strukturertDokument != null) {
            "Hvis det ikke finnes et strukturert dokument må innsendingstype være satt"
        }
    }

    override fun tilBehandlingHendelse(): BehandlingHendelse {
        return DokumentMottattBehandlingHendelse()
    }

    override fun getInnsendingType(): InnsendingType {
        return innsendingType ?: strukturertDokument?.data?.innsendingType()!!
    }

    override fun getInnsendingReferanse(): InnsendingReferanse {
        return InnsendingReferanse(journalpost)
    }

    override fun getMelding(): StrukturertDokument<*>? {
        return strukturertDokument
    }
}
