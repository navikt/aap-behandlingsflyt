package no.nav.aap.behandlingsflyt.faktagrunnlag.sykdom.flate

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.flate.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.faktagrunnlag.BehandlingReferanseService
import no.nav.aap.behandlingsflyt.faktagrunnlag.sykdom.SykdomsTjeneste
import no.nav.aap.behandlingsflyt.faktagrunnlag.yrkesskade.YrkesskadeRepository

fun NormalOpenAPIRoute.sykdomsgrunnlagApi() {
    route("/api/behandling") {
        route("/{referanse}/grunnlag/sykdom/sykdom") {
            get<BehandlingReferanse, SykdomsGrunnlagDto> { req ->
                val behandling = BehandlingReferanseService.behandling(req)

                val yrkesskadeGrunnlag = YrkesskadeRepository.hentHvisEksisterer(behandlingId = behandling.id)
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
                val behandling = BehandlingReferanseService.behandling(req)

                val yrkesskadeGrunnlag = YrkesskadeRepository.hentHvisEksisterer(behandlingId = behandling.id)
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
    }
}