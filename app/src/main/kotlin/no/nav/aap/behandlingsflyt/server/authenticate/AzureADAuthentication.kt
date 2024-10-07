package no.nav.aap.behandlingsflyt.server.authenticate

import com.papsign.ktor.openapigen.route.response.OpenAPIPipelineContext
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import no.nav.aap.komponenter.server.AZURE
import no.nav.aap.verdityper.sakogbehandling.NavIdent


fun OpenAPIPipelineContext.innloggetNavIdent(): NavIdent {
    val principal = this.pipeline.call.principal<JWTPrincipal>(AZURE)!!
    val navIdent = requireNotNull(principal["NAVident"]) {
        "NAVident mangler i token"
    }
    return NavIdent(navIdent)
}
