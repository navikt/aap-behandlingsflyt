package no.nav.aap.tilgang

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.OpenAPIPipelineResponseContext

inline fun <reified TParams : Any, reified TResponse : Any, reified TRequest : Saksreferanse> NormalOpenAPIRoute.authorizedSakPost(
    operasjon: Operasjon,
    noinline body: suspend OpenAPIPipelineResponseContext<TResponse>.(TParams, TRequest) -> Unit
) {
    ktorRoute.installerTilgangPostPlugin<TRequest>(operasjon)
    @Suppress("UnauthorizedPost")
    post<TParams, TResponse, TRequest> { params, request -> body(params, request) }
}

inline fun <reified TParams : Any, reified TResponse : Any, reified TRequest : Behandlingsreferanse> NormalOpenAPIRoute.authorizedBehandlingPost(
    operasjon: Operasjon,
    noinline body: suspend OpenAPIPipelineResponseContext<TResponse>.(TParams, TRequest) -> Unit
) {
    ktorRoute.installerTilgangBPostPlugin<TRequest>(operasjon)
    @Suppress("UnauthorizedPost")
    post<TParams, TResponse, TRequest> { params, request -> body(params, request) }
}

inline fun <reified TParams : Any, reified TResponse : Any> NormalOpenAPIRoute.authorizedGet(
    operasjon: Operasjon,
    ressurs: Ressurs,
    noinline body: suspend OpenAPIPipelineResponseContext<TResponse>.(TParams) -> Unit
) {
    ktorRoute.installerTilgangGetPlugin(operasjon, ressurs)
    @Suppress("UnauthorizedGet")
    get<TParams, TResponse> { params -> body(params) }
}

inline fun <reified TParams : Any, reified TResponse : Any> NormalOpenAPIRoute.authorizedGetWithWhitelist(
    whitelist: List<String>,
    noinline body: suspend OpenAPIPipelineResponseContext<TResponse>.(TParams) -> Unit
) {
    ktorRoute.installerTilgangGetPluginWithWhitelist(whitelist)
    @Suppress("UnauthorizedGet")
    get<TParams, TResponse> { params -> body(params) }
}