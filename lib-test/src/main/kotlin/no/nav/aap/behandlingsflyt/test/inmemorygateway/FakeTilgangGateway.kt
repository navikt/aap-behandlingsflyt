package no.nav.aap.behandlingsflyt.test.inmemorygateway

import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.tilgang.TilgangGateway
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken
import no.nav.aap.tilgang.Operasjon
import java.util.*
import no.nav.aap.behandlingsflyt.pip.IdentPåSak
import no.nav.aap.tilgang.RelevanteIdenter

object FakeTilgangGateway : TilgangGateway {
    override fun sjekkTilgangTilBehandling(
        behandlingsreferanse: UUID,
        avklaringsbehov: Definisjon,
        token: OidcToken,
        relevanteIdenter: RelevanteIdenter
    ): Boolean {
        return true
    }

    override fun sjekkTilgangTilSak(
        saksnummer: Saksnummer,
        token: OidcToken,
        operasjon: Operasjon,
        relevanteIdenter: RelevanteIdenter
    ): Boolean {
        return true
    }

    override fun sjekkTilgangTilPerson(
        ident: String,
        token: OidcToken,
        operasjon: Operasjon
    ): Boolean {
        return true
    }
}