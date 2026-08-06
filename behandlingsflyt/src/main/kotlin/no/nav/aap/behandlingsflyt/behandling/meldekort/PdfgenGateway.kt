package no.nav.aap.behandlingsflyt.behandling.meldekort

import no.nav.aap.komponenter.gateway.Gateway

interface PdfgenGateway : Gateway {
    fun genererMeldekortPdf(request: MeldekortPdfRequest): ByteArray
}
