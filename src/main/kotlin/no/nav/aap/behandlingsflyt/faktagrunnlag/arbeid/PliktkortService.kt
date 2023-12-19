package no.nav.aap.behandlingsflyt.faktagrunnlag.arbeid

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.faktagrunnlag.Grunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.Grunnlagkonstruktør
import no.nav.aap.behandlingsflyt.flyt.FlytKontekst
import no.nav.aap.behandlingsflyt.mottak.MottaDokumentService
import no.nav.aap.behandlingsflyt.mottak.MottattDokumentRepository
import no.nav.aap.behandlingsflyt.mottak.pliktkort.MottakAvPliktkortRepository

class PliktkortService private constructor(private val mottaDokumentService: MottaDokumentService) : Grunnlag {

    companion object : Grunnlagkonstruktør {
        override fun konstruer(connection: DBConnection): PliktkortService {
            return PliktkortService(
                MottaDokumentService(
                    MottattDokumentRepository(connection),
                    MottakAvPliktkortRepository(connection)
                )
            )
        }
    }

    override fun oppdater(kontekst: FlytKontekst): Boolean {
        val pliktkortSomIkkeErBehandlet = mottaDokumentService.pliktkortSomIkkeErBehandlet(kontekst.sakId)
        if (pliktkortSomIkkeErBehandlet.isEmpty()) {
            return false
        }
        return false // TODO fiks
    }
}
