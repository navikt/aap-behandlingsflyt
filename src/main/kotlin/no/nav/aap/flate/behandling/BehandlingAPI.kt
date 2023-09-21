package no.nav.aap.flate.behandling

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import com.papsign.ktor.openapigen.route.throws
import io.ktor.http.*
import no.nav.aap.domene.behandling.BehandlingTjeneste
import no.nav.aap.domene.behandling.grunnlag.sykdom.SykdomsTjeneste
import no.nav.aap.domene.behandling.grunnlag.yrkesskade.YrkesskadeTjeneste

fun NormalOpenAPIRoute.behandlingApi() {
    route("/api/behandling") {
        route("/{referanse}") {
            throws(HttpStatusCode.BadRequest, IllegalArgumentException::class) {
                throws(HttpStatusCode.NoContent, NoSuchElementException::class) {
                    get<BehandlingReferanse, DetaljertBehandlingDTO> { req ->
                        val eksternReferanse = req.ref()

                        val behandling = BehandlingTjeneste.hent(eksternReferanse)

                        val dto = DetaljertBehandlingDTO(
                            referanse = behandling.referanse,
                            type = behandling.type.identifikator(),
                            status = behandling.status(),
                            opprettet = behandling.opprettetTidspunkt,
                            avklaringsbehov = behandling.avklaringsbehov().map { avklaringsbehov ->
                                AvklaringsbehovDTO(
                                    definisjon = avklaringsbehov.definisjon,
                                    status = avklaringsbehov.status(),
                                    endringer = avklaringsbehov.historikk.map { endring ->
                                        EndringDTO(
                                            status = endring.status,
                                            tidsstempel = endring.tidsstempel,
                                            begrunnelse = endring.begrunnelse,
                                            endretAv = endring.endretAv
                                        )
                                    }
                                )
                            },
                            vilkår = behandling.vilkårsresultat().alle().map { vilkår ->
                                VilkårDTO(
                                    vilkårstype = vilkår.type,
                                    perioder = vilkår.vilkårsperioder()
                                        .map { vp ->
                                            VilkårsperiodeDTO(
                                                periode = vp.periode,
                                                utfall = vp.utfall,
                                                manuellVurdering = vp.manuellVurdering,
                                                begrunnelse = vp.begrunnelse
                                            )
                                        })
                            },
                            aktivtSteg = behandling.stegHistorikk().last().tilstand.steg()
                        )

                        respond(dto)
                    }
                }
            }
        }
        route("/{referanse}/grunnlag/sykdom") {
            get<BehandlingReferanse, SykdomsGrunnlagDto> { req ->
                val behandling = BehandlingTjeneste.hent(req.ref())

                val yrkesskadeGrunnlagOptional = YrkesskadeTjeneste.hentHvisEksisterer(behandlingId = behandling.id)
                val sykdomsGrunnlag = SykdomsTjeneste.hentHvisEksisterer(behandlingId = behandling.id)

                respond(
                    SykdomsGrunnlagDto(
                        opplysninger = InnhentetSykdomsOpplysninger(
                            oppgittYrkesskadeISøknad = false,
                            innhentedeYrkesskader = yrkesskadeGrunnlagOptional?.yrkesskader?.yrkesskader?.map { yrkesskade ->
                                RegistrertYrkesskade(
                                    ref = yrkesskade.ref,
                                    periode = yrkesskade.periode,
                                    kilde = "Yrkesskaderegisteret"
                                )
                            } ?: emptyList()
                        ),
                        yrkesskadevurdering = sykdomsGrunnlag?.yrkesskadevurdering,
                        sykdomsvurdering = sykdomsGrunnlag?.sykdomsvurdering
                    )
                )
            }
        }
        route("/{referanse}/flyt") {
            get<BehandlingReferanse, BehandlingFlytOgTilstandDto> { req ->
                val behandling = BehandlingTjeneste.hent(req.ref())

                respond(BehandlingFlytOgTilstandDto(behandling.flyt().stegene()))
            }
        }
    }
}