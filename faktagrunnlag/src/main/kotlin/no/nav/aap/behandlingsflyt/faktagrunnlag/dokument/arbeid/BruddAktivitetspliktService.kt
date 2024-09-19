package no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid

import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskravkonstruktør
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottaDokumentService
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepository
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.verdityper.dokument.JournalpostId
import no.nav.aap.verdityper.flyt.FlytKontekst

class BruddAktivitetspliktService (
    private val mottaDokumentService: MottaDokumentService,
    private val bruddAktivitetspliktRepository: BruddAktivitetspliktRepository,
) : Informasjonskrav {

    companion object : Informasjonskravkonstruktør {
        override fun konstruer(connection: DBConnection): BruddAktivitetspliktService {
            return BruddAktivitetspliktService(
                MottaDokumentService(MottattDokumentRepository(connection)),
                BruddAktivitetspliktRepository(connection)
            )
        }
    }

    override fun harIkkeGjortOppdateringNå(kontekst: FlytKontekst): Boolean {
        val aktivitetskortSomIkkeErBehandlet = mottaDokumentService.aktivitetskortSomIkkeErBehandlet(kontekst.sakId)
        if (aktivitetskortSomIkkeErBehandlet.isEmpty()) {
            return true
        }

        val eksisterendeBrudd = bruddAktivitetspliktRepository.hentGrunnlagHvisEksisterer(kontekst.behandlingId)
            ?.bruddene
            ?: emptyList()

        val alleBrudd = HashSet<BruddAktivitetsplikt>(eksisterendeBrudd)

        for (ubehandletInnsendingId in aktivitetskortSomIkkeErBehandlet) {
            val nyeBrudd = bruddAktivitetspliktRepository.hentBruddForInnsending(ubehandletInnsendingId)
            alleBrudd.addAll(nyeBrudd)
            mottaDokumentService.knyttTilBehandling(
                sakId = kontekst.sakId,
                behandlingId = kontekst.behandlingId,
                journalpostId = JournalpostId(ubehandletInnsendingId.toString())
            )
        }

        bruddAktivitetspliktRepository.nyttGrunnlag(behandlingId = kontekst.behandlingId, brudd = alleBrudd)
        return false
    }
}