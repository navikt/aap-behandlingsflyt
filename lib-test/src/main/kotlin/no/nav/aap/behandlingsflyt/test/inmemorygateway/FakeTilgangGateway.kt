package no.nav.aap.behandlingsflyt.test.inmemorygateway

import no.nav.aap.behandlingsflyt.tilgang.TilgangGateway
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken
import java.util.*

object FakeTilgangGateway : TilgangGateway {
    override fun sjekkTilgang(behandlingsreferanse: UUID, avklaringsbehovKode: String, token: OidcToken): Boolean {
        return true
    }
}