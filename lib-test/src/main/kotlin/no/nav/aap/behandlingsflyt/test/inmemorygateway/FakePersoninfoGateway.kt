package no.nav.aap.behandlingsflyt.test.inmemorygateway

import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersoninfoGateway
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.Personinfo
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken

object FakePersoninfoGateway : PersoninfoGateway {
    override fun hentPersoninfoForIdent(ident: Ident, currentToken: OidcToken): Personinfo {
        return Personinfo(
            ident = ident,
            fornavn = "Fake",
            mellomnavn = null,
            etternavn = "Person",
        )
    }
}
