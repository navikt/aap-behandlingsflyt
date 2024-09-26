package no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid

import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav.Endret.ENDRET
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav.Endret.IKKE_ENDRET
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskravkonstruktør
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentReferanse
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottaDokumentService
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepository
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.verdityper.flyt.FlytKontekstMedPerioder

class BruddAktivitetspliktService (
    private val mottaDokumentService: MottaDokumentService,
    private val bruddAktivitetspliktRepository: BruddAktivitetspliktRepository,
) : Informasjonskrav {

    companion object : Informasjonskravkonstruktør {
        override fun erRelevant(kontekst: FlytKontekstMedPerioder): Boolean {
            return true
        }

        override fun konstruer(connection: DBConnection): BruddAktivitetspliktService {
            return BruddAktivitetspliktService(
                MottaDokumentService(MottattDokumentRepository(connection)),
                BruddAktivitetspliktRepository(connection)
            )
        }
    }

    override fun oppdater(kontekst: FlytKontekstMedPerioder): Informasjonskrav.Endret {
        val aktivitetskortSomIkkeErBehandlet = mottaDokumentService.aktivitetskortSomIkkeErBehandlet(kontekst.sakId)
        if (aktivitetskortSomIkkeErBehandlet.isEmpty()) {
            return IKKE_ENDRET
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
                referanse = MottattDokumentReferanse(ubehandletInnsendingId),
            )
        }

        bruddAktivitetspliktRepository.nyttGrunnlag(behandlingId = kontekst.behandlingId, brudd = alleBrudd)
        return ENDRET
    }

}