@file:Suppress("GraphQLUnresolvedReference")

package no.nav.aap.behandlingsflyt.integrasjon.ident

import no.nav.aap.behandlingsflyt.integrasjon.pdl.IdentVariables
import no.nav.aap.behandlingsflyt.integrasjon.pdl.PdlGateway
import no.nav.aap.behandlingsflyt.integrasjon.pdl.PdlGruppe
import no.nav.aap.behandlingsflyt.integrasjon.pdl.PdlIdenterDataResponse
import no.nav.aap.behandlingsflyt.integrasjon.pdl.PdlRequest
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.IdentGateway
import no.nav.aap.komponenter.gateway.Factory
import org.intellij.lang.annotations.Language

class PdlIdentGateway : IdentGateway {

    companion object : Factory<IdentGateway> {
        override fun konstruer(): PdlIdentGateway {
            return PdlIdentGateway()
        }
    }

    override fun hentAlleIdenterForPerson(ident: Ident): List<Ident> {
        val request = PdlRequest(IDENT_QUERY, IdentVariables(ident.identifikator))
        val response: PdlIdenterDataResponse = PdlGateway.query(request)

        return response.data
            ?.hentIdenter
            ?.identer
            ?.filter { it.gruppe == PdlGruppe.FOLKEREGISTERIDENT }
            ?.map { Ident(identifikator = it.ident, aktivIdent = it.historisk.not()) }
            ?: emptyList()
    }
}


@Language("GraphQL")
val IDENT_QUERY = $$"""
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

