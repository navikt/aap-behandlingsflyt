package no.nav.aap.tilgang

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.OpenAPIPipelineResponseContext

inline fun <reified TParams : Any, reified TResponse : Any, reified TRequest : Any> NormalOpenAPIRoute.authorizedPost(
    operasjon: Operasjon,
    ressurs: Ressurs,
    avklaringsbehovKode: String? = null,
    noinline body: suspend OpenAPIPipelineResponseContext<TResponse>.(TParams, TRequest) -> Unit
) {
    ktorRoute.installerTilgangPlugin(operasjon, ressurs, avklaringsbehovKode)
    post<TParams, TResponse, TRequest> { params, request -> body(params, request) }
}

inline fun <reified TParams : Any, reified TResponse : Any> NormalOpenAPIRoute.authorizedGet(
    operasjon: Operasjon,
    ressurs: Ressurs,
    avklaringsbehovKode: String? = null,
    noinline body: suspend OpenAPIPipelineResponseContext<TResponse>.(TParams) -> Unit
) {
    ktorRoute.installerTilgangPlugin(operasjon, ressurs, avklaringsbehovKode)
    get<TParams, TResponse> { params -> body(params) }
}