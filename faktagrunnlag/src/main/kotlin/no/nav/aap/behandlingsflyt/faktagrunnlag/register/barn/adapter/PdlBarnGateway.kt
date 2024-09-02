package no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.adapter

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.Barn
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.BarnGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.Dødsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.Header
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import no.nav.aap.komponenter.httpklient.json.DefaultJsonMapper
import no.nav.aap.pdl.IdentVariables
import no.nav.aap.pdl.PdlRelasjonDataResponse
import no.nav.aap.pdl.PdlRequest
import no.nav.aap.pdl.PdlResponseHandler
import no.nav.aap.verdityper.sakogbehandling.Ident
import java.net.URI

object PdlBarnGateway : BarnGateway {

    private val url = URI.create(requiredConfigForKey("integrasjon.pdl.url"))
    val config = ClientConfig(
        scope = requiredConfigForKey("integrasjon.pdl.scope"),
        additionalHeaders = listOf(Header("Behandlingsnummer", "B287"))
    )
    private val client = RestClient(
        config = config,
        tokenProvider = ClientCredentialsTokenProvider,
        responseHandler = PdlResponseHandler()
    )

    override fun hentBarn(person: Person, relaterteBarnIdenter: List<Ident>): List<Barn> {
        val barnRelasjoner = hentBarnRelasjoner(person)
        val toMutableList = hentBarn(barnRelasjoner).toMutableList()
        toMutableList.addAll(hentBarn(relaterteBarnIdenter))
        return toMutableList
    }

    private fun hentBarnRelasjoner(person: Person): List<Ident> {
        val request = PdlRequest(BARN_RELASJON_QUERY, IdentVariables(person.aktivIdent().identifikator))
        val response: PdlRelasjonDataResponse = query(request)

        val relasjoner = (response.data?.hentPerson?.forelderBarnRelasjon ?: return emptyList())

        return relasjoner.map {
            Ident(
                it.relatertPersonsIdent
            )
        }
    }

    private fun hentBarn(identer: List<Ident>): List<Barn> {
        if (identer.isEmpty()) {
            return emptyList()
        }

        val request = PdlRequest(PERSON_BOLK_QUERY, IdentVariables(identer = identer.map { it.identifikator }))
        val response: PdlRelasjonDataResponse = query(request)

        val bolk = response.data?.hentPersonBolk ?: return emptyList()

        return bolk.mapNotNull { res ->
            res.person?.let { person ->
                person.foedselsdato?.let { foedsel ->
                    foedsel.singleOrNull()?.let { fdato ->
                        Barn(ident = Ident(res.ident),
                            fødselsdato = Fødselsdato.parse(fdato.foedselsdato.toString()),
                            dødsdato = person.doedsfall?.firstOrNull()?.doedsdato?.let { Dødsdato.parse(it) })
                    }
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

private const val ident = "\$ident"
private const val identer = "\$identer"

val BARN_RELASJON_QUERY = """
    query($ident: ID!) {
        hentPerson(ident: $ident) {
            forelderBarnRelasjon {
                relatertPersonsIdent
            }
        }
    }
""".trimIndent()

val PERSON_BOLK_QUERY = """
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
