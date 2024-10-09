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
        val erstattedeBrudd = mutableSetOf<BruddAktivitetspliktId>()

        for (ubehandletInnsendingId in aktivitetskortSomIkkeErBehandlet) {
            val nyeBrudd = bruddAktivitetspliktRepository.hentBruddForInnsending(ubehandletInnsendingId)
            alleBrudd.addAll(nyeBrudd)
            erstattedeBrudd.addAll(nyeBrudd.mapNotNull { it.erstatter })
            mottaDokumentService.knyttTilBehandling(
                sakId = kontekst.sakId,
                behandlingId = kontekst.behandlingId,
                referanse = MottattDokumentReferanse(ubehandletInnsendingId),
            )
        }

        /* Fjern erstattede brudd helt til slutt, ikke løpende. Ellers må vi
        * loope gjennom dokumentene på en måte som gjør at alle brudd legges til før de fjernes. */
        alleBrudd.removeAll { it.id in erstattedeBrudd }

        bruddAktivitetspliktRepository.nyttGrunnlag(behandlingId = kontekst.behandlingId, brudd = alleBrudd)
        return ENDRET
    }

}