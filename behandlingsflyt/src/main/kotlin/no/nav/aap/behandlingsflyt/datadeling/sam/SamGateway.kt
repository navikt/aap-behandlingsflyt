package no.nav.aap.behandlingsflyt.datadeling.sam

import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.komponenter.gateway.Gateway

interface SamGateway: Gateway {
    fun varsleVedtak(request: SamordneVedtakRequest)
    fun hentSamId(ident: Ident, sakId: String, vedtakId: String)
}