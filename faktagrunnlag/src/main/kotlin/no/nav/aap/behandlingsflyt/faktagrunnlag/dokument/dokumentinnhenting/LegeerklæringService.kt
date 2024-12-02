package no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.dokumentinnhenting

import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav.Endret.ENDRET
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav.Endret.IKKE_ENDRET
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskravkonstruktør
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottaDokumentService
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepository
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder

class LegeerklæringService private constructor(
    private val mottaDokumentService: MottaDokumentService
) : Informasjonskrav {

    companion object : Informasjonskravkonstruktør {
        override fun erRelevant(kontekst: FlytKontekstMedPerioder): Boolean {
            // Skal alltid innhentes
            return true
        }
        override fun konstruer(connection: DBConnection): LegeerklæringService {
            return LegeerklæringService(
                MottaDokumentService(
                    MottattDokumentRepository(connection)
                )
            )
        }
    }

    override fun oppdater(kontekst: FlytKontekstMedPerioder): Informasjonskrav.Endret {
        val ubehandledeLegeerklæringer = mottaDokumentService.legeerklæringerSomIkkeHarBlittBehandlet(kontekst.sakId)
        val ubehandledeDialogmeldinger = mottaDokumentService.dialogmeldingerSomIkkeHarBlittBehandlet(kontekst.sakId)

        if (ubehandledeLegeerklæringer.isEmpty() && ubehandledeDialogmeldinger.isEmpty()) {
            return IKKE_ENDRET
        }

        for (dokument in ubehandledeLegeerklæringer) {
            mottaDokumentService.knyttTilBehandling(
                sakId = kontekst.sakId,
                behandlingId = kontekst.behandlingId,
                referanse = InnsendingReferanse(dokument.journalpostId)
            )
        }

        for (dokument in ubehandledeDialogmeldinger) {
            mottaDokumentService.knyttTilBehandling(
                sakId = kontekst.sakId,
                behandlingId = kontekst.behandlingId,
                referanse = InnsendingReferanse(dokument.journalpostId)
            )
        }

        return ENDRET // Antar her at alle nye legeerklæringer gir en endring vi må ta hensyn til
    }
}