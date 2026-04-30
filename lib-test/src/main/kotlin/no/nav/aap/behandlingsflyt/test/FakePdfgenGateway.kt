package no.nav.aap.behandlingsflyt.test

import no.nav.aap.behandlingsflyt.behandling.meldekort.MeldekortPdfRequest
import no.nav.aap.behandlingsflyt.behandling.meldekort.PdfgenGateway
import no.nav.aap.komponenter.gateway.Factory

class FakePdfgenGateway : PdfgenGateway {
    override fun genererMeldekortPdf(request: MeldekortPdfRequest): ByteArray = ByteArray(0)

    companion object : Factory<PdfgenGateway> {
        override fun konstruer(): PdfgenGateway = FakePdfgenGateway()
    }
}
