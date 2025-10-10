@file:Suppress("GraphQLUnresolvedReference")

package no.nav.aap.behandlingsflyt.integrasjon.ident

import com.github.benmanes.caffeine.cache.Caffeine
import no.nav.aap.behandlingsflyt.integrasjon.pdl.IdentVariables
import no.nav.aap.behandlingsflyt.integrasjon.pdl.PdlGateway
import no.nav.aap.behandlingsflyt.integrasjon.pdl.PdlPersonNavnDataResponse
import no.nav.aap.behandlingsflyt.integrasjon.pdl.PdlRequest
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersoninfoGateway
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.Personinfo
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken
import org.intellij.lang.annotations.Language
import java.time.Duration
import java.time.LocalDate

object PdlPersoninfoGateway : PersoninfoGateway {
    private val cache = Caffeine.newBuilder()
        .maximumSize(1000)
        .expireAfterWrite(Duration.ofMinutes(15))
        .build<String, Personinfo>()

    @Language("GraphQL")
    val PERSONINFO_QUERY = $$"""
    query($ident: ID!) {
        hentPerson(ident: $ident) {
            navn(historikk: false) {
                fornavn, mellomnavn, etternavn,
            }
            foedselsdato {
                foedselsdato
            }
            doedsfall {
                doedsdato
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
        val cachetPersoninfo = cache.getIfPresent(ident.identifikator)
        if (cachetPersoninfo != null) return cachetPersoninfo

        val request = PdlRequest(PERSONINFO_QUERY, IdentVariables(ident.identifikator))
        val response: PdlPersonNavnDataResponse = PdlGateway.query(request, currentToken)
        val navn = response.data?.hentPerson?.navn?.firstOrNull()

        val foedselsdato = response.data?.hentPerson?.foedselsdato?.firstOrNull()?.foedselsdato?.let(LocalDate::parse)
        val doedsdato = response.data?.hentPerson?.doedsfall?.firstOrNull()?.doedsdato?.let(LocalDate::parse)

        return Personinfo(ident, foedselsdato, doedsdato, navn?.fornavn, navn?.mellomnavn, navn?.etternavn)
            .also { cache.put(ident.identifikator, it) }
    }

}




