package no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid

import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav.Endret.ENDRET
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav.Endret.IKKE_ENDRET
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskravkonstruktør
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottaDokumentService
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepository
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.verdityper.flyt.FlytKontekstMedPerioder

class PliktkortService private constructor(
    private val mottaDokumentService: MottaDokumentService,
    private val pliktkortRepository: PliktkortRepository
) : Informasjonskrav {

    companion object : Informasjonskravkonstruktør {
        override fun erRelevant(kontekst: FlytKontekstMedPerioder): Boolean {
            // Skal alltid innhentes
            return true
        }
        override fun konstruer(connection: DBConnection): PliktkortService {
            return PliktkortService(
                MottaDokumentService(
                    MottattDokumentRepository(connection)
                ),
                PliktkortRepository(connection)
            )
        }
    }

    override fun oppdater(kontekst: FlytKontekstMedPerioder): Informasjonskrav.Endret {
        val pliktkortSomIkkeErBehandlet = mottaDokumentService.pliktkortSomIkkeErBehandlet(kontekst.sakId)
        if (pliktkortSomIkkeErBehandlet.isEmpty()) {
            return IKKE_ENDRET
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
                referanse = InnsendingReferanse(ubehandletPliktkort.journalpostId)
            )
            allePlussNye.add(nyttPliktkort)
        }

        pliktkortRepository.lagre(behandlingId = kontekst.behandlingId, pliktkortene = allePlussNye)

        return ENDRET // Antar her at alle nye kort gir en endring vi må ta hensyn til
    }
}
