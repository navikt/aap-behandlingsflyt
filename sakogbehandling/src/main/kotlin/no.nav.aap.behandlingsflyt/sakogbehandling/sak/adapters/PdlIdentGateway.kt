package no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters

import no.nav.aap.behandlingsflyt.sakogbehandling.sak.IdentGateway
import no.nav.aap.ktor.client.AzureConfig
import no.nav.aap.pdlclient.IdentVariables
import no.nav.aap.pdlclient.PdlClient
import no.nav.aap.pdlclient.PdlConfig
import no.nav.aap.pdlclient.PdlGruppe
import no.nav.aap.pdlclient.PdlRequest
import no.nav.aap.pdlclient.PdlResponse
import no.nav.aap.verdityper.sakogbehandling.Ident
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object PdlGatewayImpl : IdentGateway {
    private lateinit var azureConfig: AzureConfig
    private lateinit var pdlConfig: PdlConfig
    private lateinit var graphQLClient: PdlClient

    fun init(
        azure: AzureConfig,
        pdl: PdlConfig
    ) {
        azureConfig = azure
        pdlConfig = pdl
        graphQLClient = PdlClient(azureConfig, pdlConfig)
    }

    // TODO: returner execption, option, result eller emptylist
    override suspend fun hentAlleIdenterForPerson(ident: Ident): List<Ident> {
        val request = PdlRequest(IDENT_QUERY, IdentVariables(ident.identifikator))
        val response: Result<PdlResponse> = graphQLClient.query(request)

        fun onSuccess(resp: PdlResponse): List<Ident> {
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

private val SECURE_LOGGER: Logger = LoggerFactory.getLogger("secureLog")
