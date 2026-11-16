package no.nav.aap.behandlingsflyt.datadeling.sam

import no.nav.aap.behandlingsflyt.behandling.vedtak.VedtakId
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.komponenter.gateway.Gateway

interface SamGateway: Gateway {
    fun varsleVedtak(request: SamordneVedtakRequest): SamordneVedtakRespons?
    fun hentSamId(ident: Ident, sakId: Long, vedtakId: VedtakId): List<SamIdOgTpNr>
}

data class SamIdOgTpNr(val samId: Long, val tpNr: Long)