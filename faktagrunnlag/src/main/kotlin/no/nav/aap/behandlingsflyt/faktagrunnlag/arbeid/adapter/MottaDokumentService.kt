package no.nav.aap.behandlingsflyt.faktagrunnlag.arbeid.adapter

import no.nav.aap.behandlingsflyt.faktagrunnlag.arbeid.DokumentType
import no.nav.aap.behandlingsflyt.faktagrunnlag.arbeid.MottattDokument
import no.nav.aap.behandlingsflyt.faktagrunnlag.arbeid.MottattDokumentRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.arbeid.Status
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import no.nav.aap.verdityper.dokument.JournalpostId
import no.nav.aap.verdityper.sakogbehandling.SakId
import java.time.LocalDateTime

class MottaDokumentService(
    private val mottattDokumentRepository: MottattDokumentRepository,
    private val pliktkortRepository: MottakAvPliktkortRepository
) {

    fun håndterMottattPliktkort(
        sakId: SakId,
        journalpostId: JournalpostId,
        mottattTidspunkt: LocalDateTime,
        pliktKort: UbehandletPliktkort
    ) {
        // Lagre data knyttet til sak
        håndterMottattDokument(journalpostId, sakId, mottattTidspunkt, DokumentType.PLIKTKORT)

        pliktkortRepository.lagre(pliktkort = pliktKort)
    }

    fun håndterMottattDokument(
        journalpostId: JournalpostId,
        sakId: SakId,
        mottattTidspunkt: LocalDateTime,
        dokumentType: DokumentType
    ) {
        mottattDokumentRepository.lagre(
            MottattDokument(
                journalpostId = journalpostId,
                sakId = sakId,
                mottattTidspunkt = mottattTidspunkt,
                type = dokumentType,
                status = Status.MOTTATT,
                behandlingId = null
            )
        )
    }

    fun pliktkortSomIkkeErBehandlet(sakId: SakId): Set<UbehandletPliktkort> {
        val ubehandledePliktkort =
            mottattDokumentRepository.hentUbehandledeDokumenterAvType(sakId, DokumentType.PLIKTKORT)

        val toSet = pliktkortRepository.hent(ubehandledePliktkort).toSet()
        if (ubehandledePliktkort.size != toSet.size) {
            throw IllegalStateException("Finner ikke data fra ubehandlede pliktkort")
        }
        return toSet
    }

    fun knyttTilBehandling(sakId: SakId, behandlingId: BehandlingId, journalpostId: JournalpostId) {
        mottattDokumentRepository.oppdaterStatus(journalpostId, behandlingId, sakId, Status.BEHANDLET)
    }
}