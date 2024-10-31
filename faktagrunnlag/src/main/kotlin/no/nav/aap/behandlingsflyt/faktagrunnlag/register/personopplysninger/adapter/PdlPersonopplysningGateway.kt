package no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.adapter

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.Dødsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Personopplysning
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonopplysningGateway
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.Header
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import no.nav.aap.komponenter.httpklient.json.DefaultJsonMapper
import no.nav.aap.pdl.IdentVariables
import no.nav.aap.pdl.PdlPersoninfoDataResponse
import no.nav.aap.pdl.PdlRequest
import no.nav.aap.pdl.PdlResponseHandler
import java.net.URI

object PdlPersonopplysningGateway : PersonopplysningGateway {

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

    private fun query(request: PdlRequest): PdlPersoninfoDataResponse {
        val httpRequest = PostRequest(body = request)
        return requireNotNull(client.post(uri = url, request = httpRequest, mapper = { body, _ ->
            DefaultJsonMapper.fromJson(body)
        }))
    }

    override fun innhent(person: Person): Personopplysning? {
        val request = PdlRequest(PERSON_QUERY, IdentVariables(person.aktivIdent().identifikator))
        val response: PdlPersoninfoDataResponse = query(request)

        val foedselsdato = PdlParser.utledFødselsdato(response.data?.hentPerson?.foedselsdato)
            ?: return null

        return Personopplysning(
            id = 0, // Setter no bs her for å få det gjennom
            fødselsdato = foedselsdato,
            dødsdato = response.data?.hentPerson?.doedsfall?.firstOrNull()?.doedsdato?.let { Dødsdato.parse(it) }
        )
    }


}

private const val ident = "\$ident"

val PERSON_QUERY = """
    query($ident: ID!){
      hentPerson(ident: $ident) {
        foedselsdato {
    	  foedselsdato
        }
      }
    }
""".trimIndent()

