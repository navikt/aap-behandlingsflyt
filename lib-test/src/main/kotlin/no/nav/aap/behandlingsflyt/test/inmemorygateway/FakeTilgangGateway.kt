package no.nav.aap.behandlingsflyt.test.inmemorygateway

import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.tilgang.TilgangGateway
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken
import no.nav.aap.tilgang.Operasjon
import java.util.*

object FakeTilgangGateway : TilgangGateway {
    override suspend fun sjekkTilgangTilBehandling(
        behandlingsreferanse: UUID,
        avklaringsbehov: Definisjon,
        token: OidcToken
    ): Boolean {
        return true
    }

    override suspend fun sjekkTilgangTilSak(saksnummer: Saksnummer, token: OidcToken, operasjon: Operasjon): Boolean {
        return true
    }

    override suspend fun sjekkTilgangTilPerson(
        ident: String,
        token: OidcToken,
        operasjon: Operasjon
    ): Boolean {
        return true
    }
}