package no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.adapter

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.Dødsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Personopplysning
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonopplysningGateway
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.IdentVariables
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.PdlPersoninfoDataResponse
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.PdlRequest
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.PdlResponseHandler
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.Header
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import no.nav.aap.komponenter.json.DefaultJsonMapper
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

    override fun innhent(person: Person, historikk: Boolean): Personopplysning? {
        val query = if (historikk) PERSON_QUERY_HISTORIKK else PERSON_QUERY

        val request = PdlRequest(query, IdentVariables(person.aktivIdent().identifikator))
        val response: PdlPersoninfoDataResponse = query(request)

        val foedselsdato = PdlParser.utledFødselsdato(response.data?.hentPerson?.foedselsdato)
            ?: return null
        val gyldigFom = response.data?.hentPerson?.statsborgerskap?.firstOrNull()?.gyldigFraOgMed ?: foedselsdato.toLocalDate()

        val status = requireNotNull(response.data?.hentPerson?.folkeregisterpersonstatus?.firstOrNull()?.status)
        val land = requireNotNull(response.data?.hentPerson?.statsborgerskap?.firstOrNull()?.land)

        return Personopplysning(
            id = 0, // Setter no bs her for å få det gjennom
            fødselsdato = foedselsdato,
            dødsdato = response.data?.hentPerson?.doedsfall?.firstOrNull()?.doedsdato?.let { Dødsdato.parse(it) },
            land = land,
            gyldigFraOgMed = gyldigFom,
            gyldigTilOgMed = response.data?.hentPerson?.statsborgerskap?.firstOrNull()?.gyldigTilOgMed,
            status = status
        )
    }
}

private const val ident = "\$ident"

val PERSON_QUERY = """
    query($ident: ID!){
      hentPerson(ident: $ident) {
        doedsfall {
            doedsdato
        },
        foedselsdato {
            foedselsdato
        },
        statsborgerskap {
            land,
            gyldigFraOgMed, 
            gyldigTilOgMed
        },
        folkeregisterpersonstatus {
            status
        }
      }
    }
""".trimIndent()

val PERSON_QUERY_HISTORIKK = """
    query($ident: ID!, historikk=true){
      hentPerson(ident: $ident) 
        foedselsdato {
    	  foedselsdato
        },
        statsborgerskap {
            land,
            gyldigFraOgMed,
            gyldigTilOgMed
        },
        folkeregisterpersonstatus {
            status
        }
      }
    }
""".trimIndent()