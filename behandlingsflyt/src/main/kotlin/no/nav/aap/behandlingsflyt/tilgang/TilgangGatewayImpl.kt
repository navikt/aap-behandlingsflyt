package no.nav.aap.behandlingsflyt.tilgang

import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken
import no.nav.aap.tilgang.BehandlingTilgangRequest
import no.nav.aap.tilgang.Operasjon
import no.nav.aap.tilgang.SakTilgangRequest
import no.nav.aap.tilgang.TilgangGateway.harTilgangTilBehandling
import no.nav.aap.tilgang.TilgangGateway.harTilgangTilSak
import java.util.UUID


object TilgangGatewayImpl : TilgangGateway {

    override fun sjekkTilgangTilBehandling(behandlingsreferanse: UUID, avklaringsbehovKode: String, token: OidcToken): Boolean {
        return harTilgangTilBehandling(
            BehandlingTilgangRequest(
                behandlingsreferanse = behandlingsreferanse,
                avklaringsbehovKode = avklaringsbehovKode,
                operasjon = Operasjon.SAKSBEHANDLE
            ), token
        )
    }

    override fun sjekkTilgangTilSak(saksnummer: Saksnummer, token: OidcToken, operasjon: Operasjon): Boolean {
        return harTilgangTilSak(
            SakTilgangRequest(
                saksnummer = saksnummer.toString(),
                operasjon = operasjon
            ), token
        )
    }
}