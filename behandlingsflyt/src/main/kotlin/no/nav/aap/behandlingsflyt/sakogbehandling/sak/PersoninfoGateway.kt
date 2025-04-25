package no.nav.aap.behandlingsflyt.sakogbehandling.sak

import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.Personinfo
import no.nav.aap.komponenter.gateway.Gateway
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken

interface PersoninfoGateway : Gateway {
    fun hentPersoninfoForIdent(ident: Ident, currentToken: OidcToken): Personinfo
}