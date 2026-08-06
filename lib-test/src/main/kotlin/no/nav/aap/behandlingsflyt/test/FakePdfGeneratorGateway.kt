package no.nav.aap.behandlingsflyt.test

import no.nav.aap.behandlingsflyt.behandling.vilkår.innsikt.Dokument
import no.nav.aap.behandlingsflyt.behandling.vilkår.innsikt.PdfGeneratorGateway
import no.nav.aap.komponenter.gateway.Factory

class FakePdfGeneratorGateway : PdfGeneratorGateway {
    override fun genererVurderingerOppsummeringDokument(request: Dokument): ByteArray {
        TODO("Not yet implemented")
    }

    companion object : Factory<PdfGeneratorGateway> {
        override fun konstruer(): PdfGeneratorGateway = FakePdfGeneratorGateway()
    }
}
