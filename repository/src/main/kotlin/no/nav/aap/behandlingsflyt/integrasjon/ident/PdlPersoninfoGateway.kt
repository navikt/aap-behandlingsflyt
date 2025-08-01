@file:Suppress("GraphQLUnresolvedReference")

package no.nav.aap.behandlingsflyt.integrasjon.ident

import no.nav.aap.behandlingsflyt.integrasjon.pdl.IdentVariables
import no.nav.aap.behandlingsflyt.integrasjon.pdl.PdlGateway
import no.nav.aap.behandlingsflyt.integrasjon.pdl.PdlPersonNavnDataResponse
import no.nav.aap.behandlingsflyt.integrasjon.pdl.PdlRequest
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersoninfoGateway
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.Personinfo
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken
import org.intellij.lang.annotations.Language

object PdlPersoninfoGateway : PersoninfoGateway {

    @Language("GraphQL")
    val PERSONINFO_QUERY = $$"""
    query($ident: ID!) {
        hentPerson(ident: $ident) {
            navn(historikk: false) {
                fornavn, mellomnavn, etternavn,
            }
        }
    }
""".trimIndent()

    @Language("GraphQL")
    val PERSONINFO_BOLK_QUERY = $$"""
        query($identer: [ID!]!) {
            hentPersonBolk(identer: $identer) {
                ident,
                person {
                    navn(historikk: false) {
                        fornavn
                        mellomnavn
                        etternavn
                    }
                },
                code
            }
        }
    """.trimIndent()

    override fun hentPersoninfoForIdent(ident: Ident, currentToken: OidcToken): Personinfo {
        val request = PdlRequest(PERSONINFO_QUERY, IdentVariables(ident.identifikator))
        val response: PdlPersonNavnDataResponse = PdlGateway.query(request, currentToken)
        val navn = response.data?.hentPerson?.navn?.firstOrNull()
        return Personinfo(ident, navn?.fornavn, navn?.mellomnavn, navn?.etternavn)
    }

}




