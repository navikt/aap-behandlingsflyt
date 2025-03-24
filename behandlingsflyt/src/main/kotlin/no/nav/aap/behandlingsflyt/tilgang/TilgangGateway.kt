package no.nav.aap.behandlingsflyt.tilgang

import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken
import no.nav.aap.lookup.gateway.Gateway
import java.util.*

interface TilgangGateway : Gateway {
    fun sjekkTilgang(behandlingsreferanse: UUID, avklaringsbehovKode: String, token: OidcToken): Boolean
}