package no.nav.aap.behandlingsflyt.grunnlag.flate

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.avklaringsbehov.vedtak.TotrinnsVurdering
import no.nav.aap.behandlingsflyt.domene.ElementNotFoundException
import no.nav.aap.behandlingsflyt.domene.behandling.Behandling
import no.nav.aap.behandlingsflyt.domene.behandling.BehandlingTjeneste
import no.nav.aap.behandlingsflyt.domene.behandling.avklaringsbehov.Avklaringsbehov
import no.nav.aap.behandlingsflyt.domene.behandling.avklaringsbehov.Status
import no.nav.aap.behandlingsflyt.flate.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.grunnlag.meldeplikt.MeldepliktTjeneste
import java.util.*

fun NormalOpenAPIRoute.behandlingsgrunnlagApi() {
    route("/api/behandling") {
        route("/{referanse}/grunnlag/medlemskap") {
            get<BehandlingReferanse, MedlemskapGrunnlagDto> { req ->
                val behandling = behandling(req)
                respond(MedlemskapGrunnlagDto())
            }
        }
        route("/{referanse}/grunnlag/fatte-vedtak") {
            get<BehandlingReferanse, FatteVedtakGrunnlagDto> { req ->
                val behandling = behandling(req)
                respond(FatteVedtakGrunnlagDto(behandling.avklaringsbehov().filter { it.erTotrinn() }
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

private fun behandling(req: BehandlingReferanse): Behandling {
    val eksternReferanse: UUID
    try {
        eksternReferanse = req.ref()
    } catch (exception: IllegalArgumentException) {
        throw ElementNotFoundException()
    }

    val behandling = BehandlingTjeneste.hent(eksternReferanse)
    return behandling
}