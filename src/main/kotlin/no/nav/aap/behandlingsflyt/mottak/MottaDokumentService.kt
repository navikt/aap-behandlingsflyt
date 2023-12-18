package no.nav.aap.behandlingsflyt.mottak

import no.nav.aap.behandlingsflyt.behandling.dokumenter.JournalpostId
import no.nav.aap.behandlingsflyt.mottak.pliktkort.MottakAvPliktkortRepository
import no.nav.aap.behandlingsflyt.mottak.pliktkort.UbehandletPliktkort
import no.nav.aap.behandlingsflyt.sak.SakId
import java.time.LocalDateTime

class MottaDokumentService(
    private val mottattDokumentRepository: MottattDokumentRepository,
    private val pliktkortRepository: MottakAvPliktkortRepository
) {

    fun håndter(
        sakId: SakId,
        journalpostId: JournalpostId,
        mottattTidspunkt: LocalDateTime,
        pliktKort: UbehandletPliktkort
    ) {
        // Lagre data knyttet til sak
        mottattDokumentRepository.lagre(
            MottattDokument(
                journalpostId = journalpostId,
                sakId = sakId,
                mottattTidspunkt = mottattTidspunkt,
                type = DokumentType.PLIKTKORT,
                behandlingId = null
            )
        )

        pliktkortRepository.lagre(pliktkort = pliktKort)
    }

    fun pliktkortSomIkkeErBehandlet(sakId: SakId): Set<UbehandletPliktkort> {
        val ubehandledePliktkort =
            mottattDokumentRepository.hentUbehandledeDokumenterAvType(sakId, DokumentType.PLIKTKORT)

        return pliktkortRepository.hent(ubehandledePliktkort).toSet()
    }
}