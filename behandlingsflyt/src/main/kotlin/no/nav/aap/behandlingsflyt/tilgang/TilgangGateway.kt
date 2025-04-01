package no.nav.aap.behandlingsflyt.tilgang

import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken
import no.nav.aap.lookup.gateway.Gateway
import java.util.*

interface TilgangGateway : Gateway {
    fun sjekkTilgangTilBehandling(behandlingsreferanse: UUID, avklaringsbehovKode: String, token: OidcToken): Boolean
    fun sjekkTilgangTilSak(saksnummer: Saksnummer, token: OidcToken): Boolean
}