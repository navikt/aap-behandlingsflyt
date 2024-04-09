package no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.adapter

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Personopplysning
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonopplysningGateway
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.httpclient.ClientConfig
import no.nav.aap.httpclient.RestClient
import no.nav.aap.httpclient.request.PostRequest
import no.nav.aap.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import no.nav.aap.pdl.IdentVariables
import no.nav.aap.pdl.PdlErrorHandler
import no.nav.aap.pdl.PdlPersoninfoDataResponse
import no.nav.aap.pdl.PdlRequest
import no.nav.aap.requiredConfigForKey
import java.net.URI
import java.time.LocalDateTime

object PdlPersonopplysningGateway : PersonopplysningGateway {

    private val url = URI.create(requiredConfigForKey("integrasjon.pdl.url"))
    val config = ClientConfig(scope = requiredConfigForKey("integrasjon.pdl.scope"))
    private val client = RestClient(
        config = config,
        tokenProvider = ClientCredentialsTokenProvider,
        errorHandler = PdlErrorHandler(config)
    )

    private fun query(request: PdlRequest): PdlPersoninfoDataResponse {
        val httpRequest = PostRequest(body = request, responseClazz = PdlPersoninfoDataResponse::class.java)
        return requireNotNull(client.post(uri = url, request = httpRequest))
    }

    override fun innhent(person: Person): Personopplysning? {
        val request = PdlRequest(PERSON_QUERY, IdentVariables(person.aktivIdent().identifikator))
        val response: PdlPersoninfoDataResponse = query(request)

        val foedselsdato = response
            .data
            ?.hentPerson
            ?.foedsel
            ?.first()
            ?.foedselsdato
            ?: return null

        return Personopplysning(
            fødselsdato = Fødselsdato.parse(foedselsdato),
            opprettetTid = LocalDateTime.now(),
        )
    }
}

private const val ident = "\$ident"

val PERSON_QUERY = """
    query($ident: ID!){
      hentPerson(ident: $ident) {
        foedsel {
    	  foedselsdato
        }
      }
    }
""".trimIndent()

