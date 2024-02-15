package no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.adapter

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.Barn
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.BarnGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.Dødsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.PdlBarnException
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.ktor.client.auth.azure.AzureConfig
import no.nav.aap.pdl.IdentVariables
import no.nav.aap.pdl.PdlClient
import no.nav.aap.pdl.PdlConfig
import no.nav.aap.pdl.PdlRequest
import no.nav.aap.pdl.PdlResponse
import no.nav.aap.verdityper.sakogbehandling.Ident

object PdlBarnGateway : BarnGateway {
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

    override suspend fun hentBarn(person: Person): List<Barn> {
        return hentBarn(hentBarnRelasjoner(person))
    }

    private suspend fun hentBarnRelasjoner(person: Person): List<Ident> {
        val request = PdlRequest(BARN_RELASJON_QUERY, IdentVariables(person.aktivIdent().identifikator))
        val response: Result<PdlResponse<PdlData>> = graphQL.query(request)

        fun onSuccess(resp: PdlResponse<PdlData>): List<Ident> {
            val relasjoner = resp
                .data
                ?.hentPerson
                ?.forelderBarnRelasjon
                ?: return emptyList()

            return relasjoner.map {
                Ident(
                    it.relatertPersonsIdent
                )
            }
        }

        fun onFailure(ex: Throwable): List<Ident> {
            throw PdlBarnException("Feil ved henting av identer for person", ex)
        }

        return response.fold(::onSuccess, ::onFailure)
    }

    private suspend fun hentBarn(identer: List<Ident>): List<Barn> {
        val request = PdlRequest(PERSON_BOLK_QUERY, IdentVariables(identer = identer.map { it.identifikator }))
        val response: Result<PdlResponse<PdlData>> = graphQL.query(request)

        fun onSuccess(resp: PdlResponse<PdlData>): List<Barn> {
            val bolk = resp
                .data
                ?.hentPersonBolk
                ?: return emptyList()

            return bolk.mapNotNull { res ->
                res.person?.let { person ->
                    person.foedsel?.let { foedsel ->
                        foedsel.singleOrNull()?.let { fdato ->
                            Barn(
                                ident = Ident(res.ident),
                                fødselsdato = Fødselsdato.parse(fdato.foedselsdato),
                                dødsdato = person.doedsfall?.first()?.doedsdato?.let { Dødsdato.parse(it) }
                            )
                        }
                    }
                }
            }
        }

        fun onFailure(ex: Throwable): List<Barn> {
            throw PdlBarnException("Feil ved henting av identer for person", ex)
        }

        return response.fold(::onSuccess, ::onFailure)
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
                foedsel {
                    foedselsdato
                }
            }
            code
        }
    }
""".trimIndent()

data class PdlData(
    val hentPerson: PdlPerson? = null,
    val hentPersonBolk: List<HentPersonBolkResult>? = null
)

data class HentPersonBolkResult(
    val ident: String,
    val person: PdlPerson? = null
)

data class PdlPerson(
    val forelderBarnRelasjon: List<PdlRelasjon>? = null,
    val foedsel: List<PdlFoedsel>? = null,
    val doedsfall: Set<PDLDødsfall>? = null
)

data class PDLDødsfall(
    val doedsdato: String
)

data class PdlFoedsel(
    val foedselsdato: String
)

data class PdlRelasjon(
    val relatertPersonsIdent: String
)