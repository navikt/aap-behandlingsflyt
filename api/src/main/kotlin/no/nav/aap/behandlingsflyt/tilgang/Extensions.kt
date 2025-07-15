package no.nav.aap.behandlingsflyt.tilgang

import com.papsign.ktor.openapigen.route.response.OpenAPIPipelineResponseContext
import io.ktor.server.application.ApplicationCall
import no.nav.aap.tilgang.kanSaksbehandleKey

fun ApplicationCall.kanSaksbehandle(): Boolean {
    val kanSaksbehandle = attributes[kanSaksbehandleKey]
    return kanSaksbehandle == "true"
}

fun <TResponse> OpenAPIPipelineResponseContext<TResponse>.kanSaksbehandle(): Boolean {
    return pipeline.call.kanSaksbehandle()
}