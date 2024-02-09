package no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.adapter

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Personopplysning
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonopplysningGateway
import no.nav.aap.ktor.client.auth.azure.AzureConfig
import no.nav.aap.pdl.IdentVariables
import no.nav.aap.pdl.PdlClient
import no.nav.aap.pdl.PdlConfig
import no.nav.aap.pdl.PdlRequest
import no.nav.aap.pdl.PdlResponse
import no.nav.aap.verdityper.sakogbehandling.Ident
import org.slf4j.Logger
import org.slf4j.LoggerFactory

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

    // TODO: returner execption, option, result eller emptylist
    override suspend fun innhent(identer: List<Ident>): Personopplysning? {
        val ident = identer
            .find { it.aktivIdent }?.identifikator
            ?:throw IllegalArgumentException()

        val request = PdlRequest(PERSON_QUERY, IdentVariables(ident))
        val response: Result<PdlResponse<PdlData>> = graphQL.query(request)

        fun onSuccess(resp: PdlResponse<PdlData>): Personopplysning? {
            val foedselsdato = resp
                .data
                ?.hentPerson
                ?.foedselsdato
                ?:throw NotImplementedError("Fant ikke fødselsdato")

            return Personopplysning(Fødselsdato.parse(foedselsdato))
        }

        fun onFailure(ex: Throwable): Personopplysning? {
            SECURE_LOGGER.error("Feil ved henting av identer for person", ex)
            return null
        }

        return response.fold(::onSuccess, ::onFailure)
    }
}

private const val ident = "\$ident"

private val PERSON_QUERY = """
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

private val SECURE_LOGGER: Logger = LoggerFactory.getLogger("secureLog")
