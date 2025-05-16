package no.nav.aap.behandlingsflyt.tilgang

import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.komponenter.gateway.Gateway
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken
import no.nav.aap.tilgang.Operasjon
import java.util.*

interface TilgangGateway : Gateway {
    fun sjekkTilgangTilBehandling(behandlingsreferanse: UUID, avklaringsbehov: Definisjon, token: OidcToken): Boolean
    fun sjekkTilgangTilSak(saksnummer: Saksnummer, token: OidcToken, operasjon: Operasjon): Boolean
}