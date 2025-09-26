package no.nav.aap.behandlingsflyt.integrasjon.ident

import no.nav.aap.behandlingsflyt.integrasjon.pdl.IdentVariables
import no.nav.aap.behandlingsflyt.integrasjon.pdl.PdlGateway
import no.nav.aap.behandlingsflyt.integrasjon.pdl.PdlNavnDataBolk
import no.nav.aap.behandlingsflyt.integrasjon.pdl.PdlPersonNavnDataResponse
import no.nav.aap.behandlingsflyt.integrasjon.pdl.PdlRequest
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersoninfoBulkGateway
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.Personinfo
import org.intellij.lang.annotations.Language

@Suppress("GraphQLUnresolvedReference")
object PdlPersoninfoBulkGateway : PersoninfoBulkGateway {

    @Language("GraphQL")
    val PERSONINFO_BOLK_QUERY = $$"""
        query($identer: [ID!]!) {
            hentPersonBolk(identer: $identer) {
                ident,
                person {
                    navn(historikk: false) {
                        fornavn
                        mellomnavn
                        etternavn
                    }
                },
                code
            }
        }
    """.trimIndent()

    override fun hentPersoninfoForIdenter(identer: List<Ident>): List<Personinfo> {
        val request = PdlRequest(PERSONINFO_BOLK_QUERY, IdentVariables(identer = identer.map { it.identifikator }))
        val response: PdlPersonNavnDataResponse = PdlGateway.query(request)

        return response.data?.hentPersonBolk?.map { person -> mapPersoninformasjon(person) }.orEmpty()
    }

    private fun mapPersoninformasjon(data: PdlNavnDataBolk): Personinfo {
        val navn = data.person?.navn?.firstOrNull()

        if (navn == null) {
            return Personinfo(Ident(data.ident!!), fornavn = "Ukjent", mellomnavn = null, etternavn = null)
        }

        return Personinfo(
            Ident(data.ident!!),
            fornavn = navn.fornavn,
            mellomnavn = navn.mellomnavn,
            etternavn = navn.etternavn
        )
    }
}




