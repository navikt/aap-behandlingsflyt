package no.nav.aap.behandlingsflyt

import io.ktor.server.application.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asContextElement
import kotlinx.coroutines.withContext
import no.nav.aap.komponenter.httpklient.auth.bruker

val ContextPlugin = createRouteScopedPlugin("ContextPlugin") {
    route?.intercept(ApplicationCallPipeline.Call) {
        try {
            val context = CallContext(call.bruker().ident)

            withContext(Dispatchers.Default + Context.asContextElement(context)) {
                proceed()
            }
        } catch (e: Exception) {
            call.application.log.error("Feil ved opprettelse av CallContext: ", e)
        } finally {
            Context.remove()
        }
    }
}
