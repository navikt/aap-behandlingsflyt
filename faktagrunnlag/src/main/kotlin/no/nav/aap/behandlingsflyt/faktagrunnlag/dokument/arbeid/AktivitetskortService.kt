package no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid

import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskravkonstruktør
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottaDokumentService
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepository
import no.nav.aap.komponenter.dbconnect.DBConnection
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
        val aktivitetskortSomIkkeErBehandlet = mottaDokumentService.pliktkortSomIkkeErBehandlet(kontekst.sakId)
        if (aktivitetskortSomIkkeErBehandlet.isEmpty()) {
            return true
        }

        val eksisterendeGrunnlag = aktivitetskortRepository.hentHvisEksisterer(kontekst.behandlingId)
        val eksisterendePliktkort = eksisterendeGrunnlag?.aktivitetskort ?: emptySet()
        val allePlussNye = HashSet<Aktivitetskort>(eksisterendePliktkort)

        for (ubehandletAktivitetskort in aktivitetskortSomIkkeErBehandlet) {
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

        aktivitetskortRepository.lagre(behandlingId = kontekst.behandlingId, pliktkortene = allePlussNye)
        return false
    }
}