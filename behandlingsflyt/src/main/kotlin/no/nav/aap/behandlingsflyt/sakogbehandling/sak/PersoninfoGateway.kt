package no.nav.aap.behandlingsflyt.sakogbehandling.sak

import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.Personinfo
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken

interface PersoninfoGateway {
    fun hentPersoninfoForIdent(ident: Ident, currentToken: OidcToken): Personinfo
}