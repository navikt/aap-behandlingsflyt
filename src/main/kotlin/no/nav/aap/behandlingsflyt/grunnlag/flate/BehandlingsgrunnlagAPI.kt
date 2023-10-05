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
import no.nav.aap.behandlingsflyt.grunnlag.sykdom.SykdomsTjeneste
import no.nav.aap.behandlingsflyt.grunnlag.yrkesskade.YrkesskadeTjeneste
import java.util.*

fun NormalOpenAPIRoute.behandlingsgrunnlagApi() {
    route("/api/behandling") {
        route("/{referanse}/grunnlag/sykdom/sykdom") {
            get<BehandlingReferanse, SykdomsGrunnlagDto> { req ->
                val behandling = behandling(req)

                val yrkesskadeGrunnlag = YrkesskadeTjeneste.hentHvisEksisterer(behandlingId = behandling.id)
                val sykdomsGrunnlag = SykdomsTjeneste.hentHvisEksisterer(behandlingId = behandling.id)

                respond(
                    SykdomsGrunnlagDto(
                        opplysninger = InnhentetSykdomsOpplysninger(
                            oppgittYrkesskadeISøknad = false,
                            innhentedeYrkesskader = yrkesskadeGrunnlag?.yrkesskader?.yrkesskader?.map { yrkesskade ->
                                RegistrertYrkesskade(
                                    ref = yrkesskade.ref,
                                    periode = yrkesskade.periode,
                                    kilde = "Yrkesskaderegisteret"
                                )
                            } ?: emptyList(),
                        ),
                        sykdomsvurdering = sykdomsGrunnlag?.sykdomsvurdering,
                            erÅrsakssammenheng = sykdomsGrunnlag?.yrkesskadevurdering?.erÅrsakssammenheng
                    )
                )
            }
        }
        route("/{referanse}/grunnlag/sykdom/yrkesskade") {
            get<BehandlingReferanse, YrkesskadeGrunnlagDto> { req ->
                val behandling = behandling(req)

                val yrkesskadeGrunnlag = YrkesskadeTjeneste.hentHvisEksisterer(behandlingId = behandling.id)
                val sykdomsGrunnlag = SykdomsTjeneste.hentHvisEksisterer(behandlingId = behandling.id)

                respond(
                    YrkesskadeGrunnlagDto(
                        opplysninger = InnhentetSykdomsOpplysninger(
                            oppgittYrkesskadeISøknad = false,
                            innhentedeYrkesskader = yrkesskadeGrunnlag?.yrkesskader?.yrkesskader?.map { yrkesskade ->
                                RegistrertYrkesskade(
                                    ref = yrkesskade.ref,
                                    periode = yrkesskade.periode,
                                    kilde = "Yrkesskaderegisteret"
                                )
                            } ?: emptyList()
                        ),
                        yrkesskadevurdering = sykdomsGrunnlag?.yrkesskadevurdering,
                    )
                )
            }
        }
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