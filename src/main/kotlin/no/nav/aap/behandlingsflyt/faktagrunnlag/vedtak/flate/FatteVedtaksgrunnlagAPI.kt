package no.nav.aap.behandlingsflyt.faktagrunnlag.vedtak.flate

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import com.zaxxer.hikari.HikariDataSource
import no.nav.aap.behandlingsflyt.avklaringsbehov.vedtak.TotrinnsVurdering
import no.nav.aap.behandlingsflyt.behandling.Behandling
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehov
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Status
import no.nav.aap.behandlingsflyt.dbstuff.transaction
import no.nav.aap.behandlingsflyt.faktagrunnlag.BehandlingReferanseService
import no.nav.aap.behandlingsflyt.flate.behandling.BehandlingReferanse

fun NormalOpenAPIRoute.fatteVedtakGrunnlagApi(dataSource: HikariDataSource) {
    route("/api/behandling") {
        route("/{referanse}/grunnlag/fatte-vedtak") {
            get<BehandlingReferanse, FatteVedtakGrunnlagDto> { req ->
                var behandling: Behandling? = null
                dataSource.transaction {
                    behandling = BehandlingReferanseService(it).behandling(req)
                }
                respond(FatteVedtakGrunnlagDto(behandling!!.avklaringsbehov().filter { it.erTotrinn() }
                    .map { tilTotrinnsVurdering(it) }))
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