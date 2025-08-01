package no.nav.aap.behandlingsflyt.integrasjon.pdl

import no.nav.aap.behandlingsflyt.integrasjon.util.GraphQLError
import java.time.LocalDate
import java.time.LocalDateTime

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
    val hentPersonBolk: List<PdlNavnDataBolk>? = null
)

data class PdlNavnData(
    val ident: String?,
    val navn: List<PdlNavn>?
)

data class PdlNavnDataBolk(
    val ident: String?,
    val person: PdlPersonBolk?
)

data class PdlPersonBolk(
    val navn: List<PdlNavn>?
)

data class PdlNavn(
    val fornavn: String?,
    val mellomnavn: String?,
    val etternavn: String?
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
    val doedsfall: Set<PDLDødsfall>? = null,
    val statsborgerskap: Set<PdlStatsborgerskap>? = null,
    val folkeregisterpersonstatus: Set<PdlFolkeregisterPersonStatus>? = null,
    val bostedsadresse: List<BostedsAdresse>? = null,
    val oppholdsadresse: List<OppholdsAdresse>? = null,
    val kontaktadresse: List<KontaktAdresse>? = null
)

data class PdlFolkeregisterPersonStatus(
    val status: PersonStatus,
    val folkeregistermetadata: PdlFolkeregistermetadata?,
)

data class PdlFolkeregistermetadata(
    val gyldighetstidspunkt: LocalDateTime?,
    val opphoerstidspunkt: LocalDateTime?
)

data class KontaktAdresse(
    val gyldigFraOgMed: LocalDateTime? = null,
    val gyldigTilOgMed: LocalDateTime? = null,
    val utenlandskAdresse: UtenlandskAdresse?,
    val utenlandskAdresseIFrittFormat: UtenlandskadresseIFrittFormat?
)

data class BostedsAdresse(
    val gyldigFraOgMed: LocalDateTime? = null,
    val gyldigTilOgMed: LocalDateTime? = null,
    val utenlandskAdresse: UtenlandskAdresse?
)

data class OppholdsAdresse(
    val gyldigFraOgMed: LocalDateTime? = null,
    val gyldigTilOgMed: LocalDateTime? = null,
    val utenlandskAdresse: UtenlandskAdresse?
)

data class UtenlandskAdresse(
    val adressenavnNummer: String?,
    val postkode: String?,
    val bySted: String?,
    val landkode: String?
)

data class UtenlandskadresseIFrittFormat(
    val adresselinje1: String?,
    val adresselinje2: String?,
    val adresselinje3: String?,
    val byEllerStedsnavn: String?,
    val postkode: String?,
    val landkode: String?
)

data class PdlStatsborgerskap(
    val land: String,
    val gyldigFraOgMed: LocalDate? = null,
    val gyldigTilOgMed: LocalDate? = null,
)

data class PDLDødsfall(
    val doedsdato: String
)


data class PdlFoedsel(
    val foedselsdato: String?,
    val foedselAar: String?
)

data class PdlRelasjon(
    val relatertPersonsIdent: String?
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

enum class PersonStatus {
    bosatt,
    utflyttet,
    forsvunnet,
    doed,
    opphort,
    foedselsregistrert,
    ikkeBosatt,
    midlertidig,
    inaktiv
}

data class GraphQLExtensions(
    val warnings: List<GraphQLWarning>?
)

//data class aGraphQLError(
//    val message: String,
//    val locations: List<GraphQLErrorLocation>,
//    val path: List<String>?,
//    val extensions: GraphQLErrorExtension
//)
//
//data class GraphQLErrorExtension(
//    val code: String?,
//    val classification: String
//)
//
//data class GraphQLErrorLocation(
//    val line: Int?,
//    val column: Int?
//)
//
class GraphQLWarning(
    val query: String?,
    val id: String?,
    val code: String?,
    val message: String?,
    val details: String?,
)