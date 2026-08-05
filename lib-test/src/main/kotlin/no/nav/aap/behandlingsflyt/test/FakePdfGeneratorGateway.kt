package no.nav.aap.behandlingsflyt.test

import no.nav.aap.behandlingsflyt.behandling.meldekort.MeldekortPdfRequest
import no.nav.aap.behandlingsflyt.behandling.meldekort.PdfGeneratorGateway
import no.nav.aap.komponenter.gateway.Factory

class FakePdfGeneratorGateway : PdfGeneratorGateway {
    override fun genererVurderingerOppsummeringDokument(request: MeldekortPdfRequest): ByteArray = ByteArray(0)

    companion object : Factory<PdfGeneratorGateway> {
        override fun konstruer(): PdfGeneratorGateway = FakePdfGeneratorGateway()
    }
}
