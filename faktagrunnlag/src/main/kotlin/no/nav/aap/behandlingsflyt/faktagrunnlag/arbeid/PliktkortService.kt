package no.nav.aap.behandlingsflyt.faktagrunnlag.arbeid

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.faktagrunnlag.arbeid.adapter.MottaDokumentService
import no.nav.aap.behandlingsflyt.faktagrunnlag.arbeid.adapter.MottakAvPliktkortRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.Grunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.Grunnlagkonstruktør
import no.nav.aap.verdityper.flyt.FlytKontekst

class PliktkortService private constructor(
    private val mottaDokumentService: MottaDokumentService,
    private val pliktkortRepository: PliktkortRepository
) : Grunnlag {

    companion object : Grunnlagkonstruktør {
        override fun konstruer(connection: DBConnection): PliktkortService {
            return PliktkortService(
                MottaDokumentService(
                    MottattDokumentRepository(connection),
                    MottakAvPliktkortRepository(connection)
                ),
                PliktkortRepository(connection)
            )
        }
    }

    override fun oppdater(kontekst: FlytKontekst): Boolean {
        val pliktkortSomIkkeErBehandlet = mottaDokumentService.pliktkortSomIkkeErBehandlet(kontekst.sakId)
        if (pliktkortSomIkkeErBehandlet.isEmpty()) {
            return false
        }

        val eksisterendeGrunnlag = pliktkortRepository.hentHvisEksisterer(kontekst.behandlingId)
        val eksisterendePliktkort = eksisterendeGrunnlag?.pliktkortene ?: emptySet()
        val allePlussNye = HashSet<Pliktkort>(eksisterendePliktkort)

        for (ubehandletPliktkort in pliktkortSomIkkeErBehandlet) {
            val nyttPliktkort = Pliktkort(
                journalpostId = ubehandletPliktkort.journalpostId,
                timerArbeidPerPeriode = ubehandletPliktkort.timerArbeidPerPeriode
            )
            mottaDokumentService.knyttTilBehandling(
                sakId = kontekst.sakId,
                behandlingId = kontekst.behandlingId,
                journalpostId = ubehandletPliktkort.journalpostId
            )
            allePlussNye.add(nyttPliktkort)
        }

        pliktkortRepository.lagre(behandlingId = kontekst.behandlingId, pliktkortene = allePlussNye)

        return true // Antar her at alle nye kort gir en endring vi må ta hensyn til
    }
}
