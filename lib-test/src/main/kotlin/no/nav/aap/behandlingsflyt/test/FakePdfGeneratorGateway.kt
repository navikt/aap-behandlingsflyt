package no.nav.aap.behandlingsflyt.test

import no.nav.aap.behandlingsflyt.behandling.vilkår.innsikt.PdfGeneratorGateway
import no.nav.aap.behandlingsflyt.behandling.vilkår.innsikt.PdfDokument
import no.nav.aap.komponenter.gateway.Factory

class FakePdfGeneratorGateway : PdfGeneratorGateway {
    override fun genererVurderingerOppsummeringPdfDokument(request: PdfDokument): ByteArray {
        return ByteArray(0)
    }

    companion object : Factory<PdfGeneratorGateway> {
        override fun konstruer(): PdfGeneratorGateway = FakePdfGeneratorGateway()
    }
}
