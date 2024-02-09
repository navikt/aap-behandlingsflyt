package no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters

import no.nav.aap.behandlingsflyt.sakogbehandling.sak.IdentGateway
import no.nav.aap.ktor.client.auth.azure.AzureConfig
import no.nav.aap.pdl.IdentVariables
import no.nav.aap.pdl.PdlClient
import no.nav.aap.pdl.PdlConfig
import no.nav.aap.pdl.PdlRequest
import no.nav.aap.pdl.PdlResponse
import no.nav.aap.verdityper.sakogbehandling.Ident
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object PdlIdentGateway : IdentGateway {
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
    override suspend fun hentAlleIdenterForPerson(ident: Ident): List<Ident> {
        val request = PdlRequest(IDENT_QUERY, IdentVariables(ident.identifikator))
        val response: Result<PdlResponse<PdlData>> = graphQL.query(request)

        fun onSuccess(resp: PdlResponse<PdlData>): List<Ident> {
            return resp.data
                ?.hentIdenter
                ?.identer
                ?.filter { it.gruppe == PdlGruppe.FOLKEREGISTERIDENT }
                ?.map { Ident(identifikator = it.ident, aktivIdent = it.historisk.not()) }
                ?: emptyList()
        }

        fun onFailure(ex: Throwable): List<Ident> {
            SECURE_LOGGER.error("Feil ved henting av identer for person", ex)
            return emptyList()
        }

        return response.fold(::onSuccess, ::onFailure)
    }
}


private const val ident = "\$ident"

private val IDENT_QUERY = """
    query($ident: ID!) {

        hentIdenter(ident: $ident, historikk: true) {
            identer {
                ident,
                historisk,
                gruppe
            }
        }
    }
""".trimIndent()

data class PdlData(
    val hentIdenter: PdlIdenter?,
)

data class PdlIdenter(
    val identer: List<PdlIdent>
)

data class PdlIdent(
    val ident: String,
    val historisk: Boolean,
    val gruppe: PdlGruppe
)

enum class PdlGruppe {
    FOLKEREGISTERIDENT,
    AKTORID,
    NPID,
}

private val SECURE_LOGGER: Logger = LoggerFactory.getLogger("secureLog")
