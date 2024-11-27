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

class AktivitetspliktInformasjonskrav (
    private val mottaDokumentService: MottaDokumentService,
    private val aktivitetspliktRepository: AktivitetspliktRepository,
) : Informasjonskrav {

    companion object : Informasjonskravkonstruktør {
        override fun erRelevant(kontekst: FlytKontekstMedPerioder): Boolean {
            return true
        }

        override fun konstruer(connection: DBConnection): AktivitetspliktInformasjonskrav {
            return AktivitetspliktInformasjonskrav(
                MottaDokumentService(MottattDokumentRepository(connection)),
                AktivitetspliktRepository(connection)
            )
        }
    }

    override fun oppdater(kontekst: FlytKontekstMedPerioder): Informasjonskrav.Endret {
        val aktivitetskortSomIkkeErBehandlet = mottaDokumentService.aktivitetskortSomIkkeErBehandlet(kontekst.sakId)
        if (aktivitetskortSomIkkeErBehandlet.isEmpty()) {
            return IKKE_ENDRET
        }

        val eksisterendeBrudd = aktivitetspliktRepository.hentGrunnlagHvisEksisterer(kontekst.behandlingId)
            ?.bruddene
            ?: emptyList()

        val alleBrudd = HashSet<AktivitetspliktDokument>(eksisterendeBrudd)

        for (ubehandletInnsendingId in aktivitetskortSomIkkeErBehandlet) {
            val nyeBrudd = aktivitetspliktRepository.hentBruddForInnsending(ubehandletInnsendingId)
            alleBrudd.addAll(nyeBrudd)
            mottaDokumentService.knyttTilBehandling(
                sakId = kontekst.sakId,
                behandlingId = kontekst.behandlingId,
                referanse = InnsendingReferanse(ubehandletInnsendingId),
            )
        }

        aktivitetspliktRepository.nyttGrunnlag(behandlingId = kontekst.behandlingId, brudd = alleBrudd)
        return ENDRET
    }
}