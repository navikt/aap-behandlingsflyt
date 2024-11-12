package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.flate

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import com.zaxxer.hikari.HikariDataSource
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.UføreRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningVurderingRepository
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.komponenter.dbconnect.transaction

fun NormalOpenAPIRoute.beregningVurderingAPI(dataSource: HikariDataSource) {
    route("/api/behandling") {
        route("/{referanse}/grunnlag/beregningsvurdering") {
            get<BehandlingReferanse, BeregningTidspunktAvklaringDto> { req ->
                val responsDto = dataSource.transaction(readOnly = true) {
                    val behandling = BehandlingReferanseService(BehandlingRepositoryImpl(it)).behandling(req)
                    val skalVurdereUføre = UføreRepository(it).hentHvisEksisterer(behandling.id)?.vurdering != null
                    val beregningGrunnlag =
                        BeregningVurderingRepository(it).hentHvisEksisterer(behandlingId = behandling.id)

                    BeregningTidspunktAvklaringDto(
                        vurdering = beregningGrunnlag?.tidspunktVurdering,
                        skalVurdereYtterligere = skalVurdereUføre
                    )
                }

                respond(
                    responsDto
                )
            }
        }
    }
}