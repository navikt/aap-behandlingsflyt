package no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.adapter

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PdlPersonopplysningException
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Personopplysning
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonopplysningGateway
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.ktor.client.auth.azure.AzureConfig
import no.nav.aap.pdl.IdentVariables
import no.nav.aap.pdl.PdlClient
import no.nav.aap.pdl.PdlConfig
import no.nav.aap.pdl.PdlRequest
import no.nav.aap.pdl.PdlResponse
import java.time.LocalDateTime

object PdlPersonopplysningGateway : PersonopplysningGateway {
    private lateinit var azureConfig: AzureConfig
    private lateinit var pdlConfig: PdlConfig
    private lateinit var graphQL: PdlClient

    fun init(
        azure: AzureConfig,
        pdl: PdlConfig
    ) {
        azureConfig = azure
        pdlConfig = pdl
        graphQL = PdlClient(azureConfig, pdlConfig)
    }

    override suspend fun innhent(person: Person): Personopplysning? {
        val request = PdlRequest(PERSON_QUERY, IdentVariables(person.aktivIdent().identifikator))
        val response: Result<PdlResponse<PdlData>> = graphQL.query(request)

        fun onSuccess(resp: PdlResponse<PdlData>): Personopplysning? {
            val foedselsdato = resp
                .data
                ?.hentPerson
                ?.foedselsdato
                ?: return null

            return Personopplysning(
                fødselsdato = Fødselsdato.parse(foedselsdato),
                opprettetTid = LocalDateTime.now(),
            )
        }

        fun onFailure(ex: Throwable): Personopplysning? {
            throw PdlPersonopplysningException("Feil ved henting av identer for person", ex)
        }

        return response.fold(::onSuccess, ::onFailure)
    }
}

private const val ident = "\$ident"

val PERSON_QUERY = """
    query($ident: ID!){
      hentPerson(ident: $ident) {
    	foedselsdato
      }
    }
""".trimIndent()

data class PdlData(
    val hentPerson: PdlPerson?,
)

data class PdlPerson(
    val foedselsdato: String
)