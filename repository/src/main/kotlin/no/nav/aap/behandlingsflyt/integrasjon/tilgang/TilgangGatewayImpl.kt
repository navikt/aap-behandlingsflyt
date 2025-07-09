package no.nav.aap.behandlingsflyt.integrasjon.tilgang

import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.tilgang.TilgangGateway
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken
import no.nav.aap.tilgang.BehandlingTilgangRequest
import no.nav.aap.tilgang.Operasjon
import no.nav.aap.tilgang.SakTilgangRequest
import java.util.UUID

object TilgangGatewayImpl : TilgangGateway {

    override fun sjekkTilgangTilBehandling(
        behandlingsreferanse: UUID,
        avklaringsbehov: Definisjon,
        token: OidcToken
    ): Boolean {
        return no.nav.aap.tilgang.TilgangGateway.harTilgangTilBehandling(
            BehandlingTilgangRequest(
                behandlingsreferanse = behandlingsreferanse,
                avklaringsbehovKode = avklaringsbehov.kode.toString(),
                operasjon = Operasjon.SAKSBEHANDLE
            ), token
        ).tilgang
    }

    override fun sjekkTilgangTilSak(saksnummer: Saksnummer, token: OidcToken, operasjon: Operasjon): Boolean {
        return no.nav.aap.tilgang.TilgangGateway.harTilgangTilSak(
            SakTilgangRequest(
                saksnummer = saksnummer.toString(),
                operasjon = operasjon
            ), token
        ).tilgang
    }
}