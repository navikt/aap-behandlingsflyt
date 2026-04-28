@file:Suppress("GraphQLUnresolvedReference")

package no.nav.aap.behandlingsflyt.integrasjon.ident

import com.github.benmanes.caffeine.cache.Caffeine
import io.micrometer.core.instrument.binder.cache.CaffeineCacheMetrics
import no.nav.aap.behandlingsflyt.integrasjon.pdl.IdentVariables
import no.nav.aap.behandlingsflyt.integrasjon.pdl.PdlGateway
import no.nav.aap.behandlingsflyt.integrasjon.pdl.PdlPersonNavnDataResponse
import no.nav.aap.behandlingsflyt.integrasjon.pdl.PdlRequest
import no.nav.aap.behandlingsflyt.prometheus
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersoninfoGateway
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.Personinfo
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken
import org.intellij.lang.annotations.Language
import java.time.Duration
import java.time.LocalDate

object PdlPersoninfoGateway : PersoninfoGateway {

    private val personinfoCache = Caffeine.newBuilder()
        .maximumSize(1_000)
        .expireAfterWrite(Duration.ofMinutes(30))
        .recordStats()
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

    init {
        CaffeineCacheMetrics.monitor(prometheus, personinfoCache, "pdl_personinfo")
    }

    override fun hentPersoninfoForIdent(ident: Ident, currentToken: OidcToken): Personinfo {
        return personinfoCache.get(ident.identifikator) {
            val request = PdlRequest(PERSONINFO_QUERY, IdentVariables(ident.identifikator))
            val person = PdlGateway.query<PdlPersonNavnDataResponse>(request, currentToken).data?.hentPerson

            val navn = person?.navn?.firstOrNull()
            val foedselsdato = person?.foedselsdato?.firstOrNull()?.foedselsdato?.let(LocalDate::parse)
            val doedsdato = person?.doedsfall?.firstOrNull()?.doedsdato?.let(LocalDate::parse)

            Personinfo(ident, foedselsdato, doedsdato, navn?.fornavn, navn?.mellomnavn, navn?.etternavn)
        }
    }

}
