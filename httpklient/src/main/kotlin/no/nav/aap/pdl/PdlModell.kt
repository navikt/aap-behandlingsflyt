package no.nav.aap.pdl

data class PdlRequest(
    val query: String,
    val variables: IdentVariables
)

data class IdentVariables(
    val ident: String? = null,
    val identer: List<String>? = null
)

data class PdlRelasjonDataResponse(
    val data: PdlRelasjonData?,
    val errors: List<GraphQLError>?,
    val extensions: GraphQLExtensions?
)

data class PdlPersoninfoDataResponse(
    val data: PdlPersoninfoData?,
    val errors: List<GraphQLError>?,
    val extensions: GraphQLExtensions?
)

data class PdlIdenterDataResponse(
    val data: PdlIdenterData?,
    val errors: List<GraphQLError>?,
    val extensions: GraphQLExtensions?
)

data class GraphQLError(
    val message: String,
    val locations: List<GraphQLErrorLocation>,
    val path: List<String>?,
    val extensions: GraphQLErrorExtension
)

data class GraphQLErrorExtension(
    val code: String?,
    val classification: String
)

data class GraphQLErrorLocation(
    val line: Int?,
    val column: Int?
)

data class GraphQLExtensions(
    val warnings: List<GraphQLWarning>?
)

class GraphQLWarning(
    val query: String?,
    val id: String?,
    val code: String?,
    val message: String?,
    val details: String?,
)

data class PdlRelasjonData(
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

data class PdlPersoninfoData(
    val hentPerson: PdlPersoninfo?,
)

data class PdlPersoninfo(
    val foedselsdato: String
)

data class PdlIdenterData(
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