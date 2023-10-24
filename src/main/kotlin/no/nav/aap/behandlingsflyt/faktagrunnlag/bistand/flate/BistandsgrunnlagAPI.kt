package no.nav.aap.behandlingsflyt.faktagrunnlag.bistand.flate

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.flate.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.faktagrunnlag.BehandlingReferanseService
import no.nav.aap.behandlingsflyt.faktagrunnlag.bistand.BistandsTjeneste

fun NormalOpenAPIRoute.bistandsgrunnlagApi() {
    route("/api/behandling") {
        route("/{referanse}/grunnlag/bistand") {
            get<BehandlingReferanse, BistandGrunnlagDto> { req ->
                val behandling = BehandlingReferanseService.behandling(req)

                val bistandsGrunnlag = BistandsTjeneste.hentHvisEksisterer(behandling.id)
                respond(BistandGrunnlagDto(bistandsGrunnlag?.vurdering))
            }
        }
    }
}