@file:Suppress("GraphQLUnresolvedReference")

package no.nav.aap.behandlingsflyt.integrasjon.barn

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.BarnFraRegister
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.BarnGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.Dødsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.adapter.BarnInnhentingRespons
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.adapter.PdlParser
import no.nav.aap.behandlingsflyt.prometheus
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.IdentVariables
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.PdlRelasjonDataResponse
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.PdlRequest
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.PdlResponseHandler
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.gateway.Factory
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.Header
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import no.nav.aap.komponenter.json.DefaultJsonMapper
import org.intellij.lang.annotations.Language
import java.net.URI

class PdlBarnGateway : BarnGateway {

    private val url = URI.create(requiredConfigForKey("integrasjon.pdl.url"))
    private val config = ClientConfig(
        scope = requiredConfigForKey("integrasjon.pdl.scope"),
        additionalHeaders = listOf(Header("Behandlingsnummer", "B287"))
    )
    private val client = RestClient(
        config = config,
        tokenProvider = ClientCredentialsTokenProvider,
        responseHandler = PdlResponseHandler(),
        prometheus = prometheus
    )

    companion object : Factory<BarnGateway> {
        override fun konstruer(): BarnGateway {
            return PdlBarnGateway()
        }
    }

    override fun hentBarn(person: Person, oppgitteBarnIdenter: List<Ident>): BarnInnhentingRespons {
        val barnRelasjoner = hentBarnRelasjoner(person)
        val registerBarn = hentBarn(barnRelasjoner)
        val oppgitteBarn = hentBarn(oppgitteBarnIdenter)
        return BarnInnhentingRespons(registerBarn, oppgitteBarn)
    }

    private fun hentBarnRelasjoner(person: Person): List<Ident> {
        val request = PdlRequest(BARN_RELASJON_QUERY, IdentVariables(person.aktivIdent().identifikator))
        val response: PdlRelasjonDataResponse = query(request)

        val relasjoner = (response.data?.hentPerson?.forelderBarnRelasjon ?: return emptyList())

        return relasjoner.map {
            Ident(
                requireNotNull(it.relatertPersonsIdent) { "Vi støtter ikke per nå at denne er null fra PDL " }
            )
        }
    }

    private fun hentBarn(identer: List<Ident>): List<BarnFraRegister> {
        if (identer.isEmpty()) {
            return emptyList()
        }

        val request = PdlRequest(PERSON_BOLK_QUERY, IdentVariables(identer = identer.map { it.identifikator }))
        val response: PdlRelasjonDataResponse = query(request)

        val bolk = response.data?.hentPersonBolk ?: return emptyList()

        return bolk.mapNotNull { res ->
            res.person?.let { person ->
                person.foedselsdato?.let { foedsel ->
                    val fødselsdato = PdlParser.utledFødselsdato(foedsel)
                    BarnFraRegister(
                        ident = Ident(res.ident),
                        fødselsdato = requireNotNull(fødselsdato),
                        dødsdato = person.doedsfall?.firstOrNull()?.doedsdato?.let { Dødsdato.parse(it) })
                }
            }
        }
    }

    private fun query(request: PdlRequest): PdlRelasjonDataResponse {
        val httpRequest = PostRequest(body = request)
        return requireNotNull(client.post(uri = url, request = httpRequest, mapper = { body, _ ->
            DefaultJsonMapper.fromJson(body)
        }))
    }
}

@Language("GraphQL")
val BARN_RELASJON_QUERY = $$"""
    query($ident: ID!) {
        hentPerson(ident: $ident) {
            forelderBarnRelasjon {
                relatertPersonsIdent
            }
        }
    }
""".trimIndent()

@Language("GraphQL")
val PERSON_BOLK_QUERY = $$"""
    query($identer: [ID!]!) {
        hentPersonBolk(identer: $identer) {
            ident,
            person {
                doedsfall {
                    doedsdato
                },
                foedselsdato {
                    foedselsdato
                }
            }
            code
        }
    }
""".trimIndent()
