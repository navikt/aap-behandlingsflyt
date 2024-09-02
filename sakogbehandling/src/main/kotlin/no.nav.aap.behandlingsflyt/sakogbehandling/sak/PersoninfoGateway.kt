package no.nav.aap.behandlingsflyt.sakogbehandling.sak

import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.Personinfo
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken
import no.nav.aap.verdityper.sakogbehandling.Ident

interface PersoninfoGateway {
    fun hentPersoninfoForIdent(ident: Ident, currentToken: OidcToken): Personinfo
}