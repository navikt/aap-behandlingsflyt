package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.flate

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import com.zaxxer.hikari.HikariDataSource
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningVurderingRepository
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.komponenter.dbconnect.transaction

fun NormalOpenAPIRoute.beregningVurderingAPI(dataSource: HikariDataSource) {
    route("/api/behandling") {
        route("/{referanse}/grunnlag/beregningsvurdering") {
            get<BehandlingReferanse, BeregningsVurderingDTO> { req ->
                val behandling: Behandling = dataSource.transaction {
                    BehandlingReferanseService(BehandlingRepositoryImpl(it)).behandling(req)
                }

                val beregningVurdering = dataSource.transaction {
                    BeregningVurderingRepository(it).hentHvisEksisterer(behandlingId = behandling.id)
                }

                val beregningsVurderingDTO = BeregningsVurderingDTO(
                    beregningVurdering?.begrunnelse,
                    beregningVurdering?.ytterligereNedsattArbeidsevneDato,
                    beregningVurdering?.antattÅrligInntekt?.verdi()
                )


                respond(
                    beregningsVurderingDTO
                )
            }
        }
    }
}