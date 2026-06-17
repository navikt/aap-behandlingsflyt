package no.nav.aap.behandlingsflyt.integrasjon.tilgang

import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.tilgang.TilgangGateway
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken
import no.nav.aap.tilgang.BehandlingTilgangRequest
import no.nav.aap.tilgang.Operasjon
import no.nav.aap.tilgang.PersonTilgangRequest
import no.nav.aap.tilgang.RelevanteIdenter
import no.nav.aap.tilgang.SakTilgangRequest
import java.util.*

object TilgangGatewayImpl : TilgangGateway {

    override suspend fun sjekkTilgangTilBehandling(
        behandlingsreferanse: UUID,
        avklaringsbehov: Definisjon,
        token: OidcToken,
        relevanteIdenter: RelevanteIdenter
    ): Boolean {
        return no.nav.aap.tilgang.TilgangGateway.harTilgangTilBehandling(
            BehandlingTilgangRequest(
                behandlingsreferanse = behandlingsreferanse,
                påkrevdRolle = avklaringsbehov.løsesAv,
                operasjon = Operasjon.SAKSBEHANDLE,
                relevanteIdenter = relevanteIdenter
            ), token
        ).tilgang
    }

    override suspend fun sjekkTilgangTilSak(
        saksnummer: Saksnummer,
        token: OidcToken,
        operasjon: Operasjon,
        relevanteIdenter: RelevanteIdenter
    ): Boolean {
        return no.nav.aap.tilgang.TilgangGateway.harTilgangTilSak(
            SakTilgangRequest(
                saksnummer = saksnummer.toString(),
                operasjon = operasjon,
                relevanteIdenter = relevanteIdenter
            ), token
        ).tilgang
    }

    override suspend fun sjekkTilgangTilPerson(ident: String, token: OidcToken, operasjon: Operasjon): Boolean {
        return no.nav.aap.tilgang.TilgangGateway.harTilgangTilPerson(
            body = PersonTilgangRequest(ident),
            currentToken = token
        ).tilgang
    }
}