package no.nav.aap.behandlingsflyt.faktagrunnlag.vedtak.flate

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import com.zaxxer.hikari.HikariDataSource
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.vedtak.TotrinnsVurdering
import no.nav.aap.behandlingsflyt.behandling.Behandling
import no.nav.aap.behandlingsflyt.avklaringsbehov.Avklaringsbehov
import no.nav.aap.behandlingsflyt.avklaringsbehov.AvklaringsbehovRepositoryImpl
import no.nav.aap.behandlingsflyt.avklaringsbehov.Status
import no.nav.aap.behandlingsflyt.behandling.flate.BehandlingReferanse
import no.nav.aap.behandlingsflyt.dbconnect.transaction
import no.nav.aap.behandlingsflyt.behandling.flate.BehandlingReferanseService

fun NormalOpenAPIRoute.fatteVedtakGrunnlagApi(dataSource: HikariDataSource) {
    route("/api/behandling") {
        route("/{referanse}/grunnlag/fatte-vedtak") {
            get<BehandlingReferanse, FatteVedtakGrunnlagDto> { req ->
                val behandling: Behandling = dataSource.transaction {
                    BehandlingReferanseService(it).behandling(req)
                }
                val dto = dataSource.transaction { connection ->
                    val avklaringsbehovene = AvklaringsbehovRepositoryImpl(connection).hentAvklaringsbehovene(behandling.id)

                    FatteVedtakGrunnlagDto(avklaringsbehovene.alle()
                        .filter { it.erTotrinn() }
                        .map { tilTotrinnsVurdering(it) })
                }
                respond(dto)
            }
        }
    }
}

private fun tilTotrinnsVurdering(it: Avklaringsbehov): TotrinnsVurdering {
    return if (it.erTotrinnsVurdert()) {
        val sisteVurdering =
            it.historikk.lastOrNull { it.status in setOf(Status.SENDT_TILBAKE_FRA_BESLUTTER, Status.TOTRINNS_VURDERT) }
        val godkjent = it.status() == Status.TOTRINNS_VURDERT

        TotrinnsVurdering(it.definisjon.kode, godkjent, sisteVurdering?.begrunnelse)
    } else {
        TotrinnsVurdering(it.definisjon.kode, null, null)
    }
}