package no.nav.aap.pdl

data class PdlRequest(
    val query: String,
    val variables: IdentVariables
)

data class IdentVariables(
    val ident: String? = null,
    val identer: List<String>? = null
)

abstract class PdlResponse(
    val errors: List<GraphQLError>?,
    val extensions: GraphQLExtensions?
)

class PdlRelasjonDataResponse(
    val data: PdlRelasjonData?,
    errors: List<GraphQLError>?,
    extensions: GraphQLExtensions?
) : PdlResponse(errors, extensions)

class PdlPersoninfoDataResponse(
    val data: PdlPersoninfoData?,
    errors: List<GraphQLError>?,
    extensions: GraphQLExtensions?
) : PdlResponse(errors, extensions)

class PdlIdenterDataResponse(
    val data: PdlIdenterData?,
    errors: List<GraphQLError>?,
    extensions: GraphQLExtensions?
) : PdlResponse(errors, extensions)

class PdlPersonNavnDataResponse(
    val data: HentPerson?,
    errors: List<GraphQLError>?,
    extensions: GraphQLExtensions?
) : PdlResponse(errors, extensions)

data class HentPerson(
    val hentPerson: PdlNavnData? = null,
    val hentPersonBolk: List<PdlNavnData>? = null
)
data class PdlNavnData(
    val ident: String?,
    val navn: List<PdlNavn>?
)

data class PdlNavn(
    val fornavn: String?,
    val mellomnavn: String?,
    val etternavn: String?
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
    val hentPerson: PdlPersoninfo? = null,
    val hentPersonBolk: List<HentPersonBolkResult>? = null
)

data class HentPersonBolkResult(
    val ident: String,
    val person: PdlPersoninfo? = null
)

data class PdlPersoninfo(
    val forelderBarnRelasjon: List<PdlRelasjon>? = null,
    val foedselsdato: List<PdlFoedsel>? = null,
    val doedsfall: Set<PDLDødsfall>? = null
)

data class PDLDødsfall(
    val doedsdato: String
)

data class PdlFoedsel(
    val foedselsdato: String?,
    val foedselAar: String?
)

data class PdlRelasjon(
    val relatertPersonsIdent: String
)

data class PdlPersoninfoData(
    val hentPerson: PdlPersoninfo?,
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