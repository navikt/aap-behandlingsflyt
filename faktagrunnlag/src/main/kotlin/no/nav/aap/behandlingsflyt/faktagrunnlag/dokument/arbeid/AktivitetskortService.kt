package no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid

import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskravkonstruktør
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottaDokumentService
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepository
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.verdityper.dokument.JournalpostId
import no.nav.aap.verdityper.flyt.FlytKontekst

class AktivitetskortService (
    private val mottaDokumentService: MottaDokumentService,
    private val aktivitetskortRepository: AktivitetskortRepository
) : Informasjonskrav {

    companion object : Informasjonskravkonstruktør {
        override fun konstruer(connection: DBConnection): AktivitetskortService {
            return AktivitetskortService(
                MottaDokumentService(MottattDokumentRepository(connection)),
                AktivitetskortRepository(connection)
            )
        }
    }

    override fun harIkkeGjortOppdateringNå(kontekst: FlytKontekst): Boolean {
        val aktivitetskortSomIkkeErBehandlet = mottaDokumentService.aktivitetskortSomIkkeErBehandlet(kontekst.sakId)
        if (aktivitetskortSomIkkeErBehandlet.isEmpty()) {
            return true
        }

        val eksisterendeGrunnlag = aktivitetskortRepository.hentHvisEksisterer(kontekst.behandlingId)
        val eksisterendeAktivitetskort = eksisterendeGrunnlag?.aktivitetskortene ?: emptySet()
        val allePlussNye = HashSet<Aktivitetskort>(eksisterendeAktivitetskort)

        for (ubehandlet in aktivitetskortSomIkkeErBehandlet) {
            val nyttAktivitetskort = Aktivitetskort(
                journalpostId = ubehandlet
            )
            mottaDokumentService.knyttTilBehandling(
                sakId = kontekst.sakId,
                behandlingId = kontekst.behandlingId,
                journalpostId = JournalpostId(ubehandlet.toString())
            )
            allePlussNye.add(nyttAktivitetskort)
        }

        aktivitetskortRepository.lagre(behandlingId = kontekst.behandlingId, aktivitetskortene = allePlussNye)
        return false
    }
}