package no.nav.aap.behandlingsflyt.flate.behandling

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.domene.ElementNotFoundException
import no.nav.aap.behandlingsflyt.domene.behandling.Behandling
import no.nav.aap.behandlingsflyt.domene.behandling.BehandlingTjeneste
import no.nav.aap.behandlingsflyt.domene.behandling.Vilkår
import no.nav.aap.behandlingsflyt.domene.behandling.Vilkårsresultat
import no.nav.aap.behandlingsflyt.domene.behandling.Vilkårstype
import no.nav.aap.behandlingsflyt.domene.behandling.grunnlag.sykdom.SykdomsTjeneste
import no.nav.aap.behandlingsflyt.domene.behandling.grunnlag.yrkesskade.YrkesskadeTjeneste
import no.nav.aap.flyt.StegGruppe
import no.nav.aap.flyt.StegType
import java.util.*

fun hentUtRelevantVilkårForSteg(vilkårsresultat: Vilkårsresultat, stegType: StegType): no.nav.aap.behandlingsflyt.flate.behandling.VilkårDTO? {
    var vilkår: Vilkår? = null
    if (stegType == StegType.AVKLAR_SYKDOM) {
        vilkår = vilkårsresultat.finnVilkår(Vilkårstype.SYKDOMSVILKÅRET)
    }
    if (stegType == StegType.VURDER_ALDER) {
        vilkår = vilkårsresultat.finnVilkår(Vilkårstype.ALDERSVILKÅRET)
    }
    if (vilkår == null) {
        return null
    }
    return no.nav.aap.behandlingsflyt.flate.behandling.VilkårDTO(
        vilkår.type,
        perioder = vilkår.vilkårsperioder().map { vp ->
            no.nav.aap.behandlingsflyt.flate.behandling.VilkårsperiodeDTO(
                vp.periode,
                vp.utfall,
                vp.manuellVurdering,
                vp.begrunnelse,
                vp.avslagsårsak
            )
        })
}

fun NormalOpenAPIRoute.behandlingApi() {
    route("/api/behandling") {
        route("/{referanse}") {
            get<no.nav.aap.behandlingsflyt.flate.behandling.BehandlingReferanse, no.nav.aap.behandlingsflyt.flate.behandling.DetaljertBehandlingDTO> { req ->
                val behandling = no.nav.aap.behandlingsflyt.flate.behandling.behandling(req)

                val dto = no.nav.aap.behandlingsflyt.flate.behandling.DetaljertBehandlingDTO(
                    referanse = behandling.referanse,
                    type = behandling.type.identifikator(),
                    status = behandling.status(),
                    opprettet = behandling.opprettetTidspunkt,
                    avklaringsbehov = behandling.avklaringsbehov().map { avklaringsbehov ->
                        no.nav.aap.behandlingsflyt.flate.behandling.AvklaringsbehovDTO(
                            definisjon = avklaringsbehov.definisjon,
                            status = avklaringsbehov.status(),
                            endringer = avklaringsbehov.historikk.map { endring ->
                                no.nav.aap.behandlingsflyt.flate.behandling.EndringDTO(
                                    status = endring.status,
                                    tidsstempel = endring.tidsstempel,
                                    begrunnelse = endring.begrunnelse,
                                    endretAv = endring.endretAv
                                )
                            }
                        )
                    },
                    vilkår = behandling.vilkårsresultat().alle().map { vilkår ->
                        no.nav.aap.behandlingsflyt.flate.behandling.VilkårDTO(
                            vilkårstype = vilkår.type,
                            perioder = vilkår.vilkårsperioder()
                                .map { vp ->
                                    no.nav.aap.behandlingsflyt.flate.behandling.VilkårsperiodeDTO(
                                        periode = vp.periode,
                                        utfall = vp.utfall,
                                        manuellVurdering = vp.manuellVurdering,
                                        begrunnelse = vp.begrunnelse,
                                        avslagsårsak = vp.avslagsårsak
                                    )
                                })
                    },
                    aktivtSteg = behandling.stegHistorikk().last().tilstand.steg()
                )

                respond(dto)
            }
        }
        route("/{referanse}/grunnlag/sykdom") {
            get<no.nav.aap.behandlingsflyt.flate.behandling.BehandlingReferanse, no.nav.aap.behandlingsflyt.flate.behandling.SykdomsGrunnlagDto> { req ->
                val behandling = no.nav.aap.behandlingsflyt.flate.behandling.behandling(req)

                val yrkesskadeGrunnlagOptional = YrkesskadeTjeneste.hentHvisEksisterer(behandlingId = behandling.id)
                val sykdomsGrunnlag = SykdomsTjeneste.hentHvisEksisterer(behandlingId = behandling.id)

                respond(
                    no.nav.aap.behandlingsflyt.flate.behandling.SykdomsGrunnlagDto(
                        opplysninger = no.nav.aap.behandlingsflyt.flate.behandling.InnhentetSykdomsOpplysninger(
                            oppgittYrkesskadeISøknad = false,
                            innhentedeYrkesskader = yrkesskadeGrunnlagOptional?.yrkesskader?.yrkesskader?.map { yrkesskade ->
                                no.nav.aap.behandlingsflyt.flate.behandling.RegistrertYrkesskade(
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
        route("/{referanse}/grunnlag/medlemskap") {
            get<no.nav.aap.behandlingsflyt.flate.behandling.BehandlingReferanse, no.nav.aap.behandlingsflyt.flate.behandling.MedlemskapGrunnlagDto> { req ->
                val behandling = no.nav.aap.behandlingsflyt.flate.behandling.behandling(req)
                respond(no.nav.aap.behandlingsflyt.flate.behandling.MedlemskapGrunnlagDto())
            }
        }
        route("/{referanse}/flyt") {
            get<no.nav.aap.behandlingsflyt.flate.behandling.BehandlingReferanse, no.nav.aap.behandlingsflyt.flate.behandling.BehandlingFlytOgTilstandDto> { req ->
                val behandling = no.nav.aap.behandlingsflyt.flate.behandling.behandling(req)

                respond(
                    no.nav.aap.behandlingsflyt.flate.behandling.BehandlingFlytOgTilstandDto(
                        behandling.flyt().stegene().map { stegType ->
                            no.nav.aap.behandlingsflyt.flate.behandling.FlytSteg(
                                stegType,
                                behandling.avklaringsbehov()
                                    .filter { avklaringsbehov -> avklaringsbehov.skalLøsesISteg(stegType) }
                                    .map { behov ->
                                        no.nav.aap.behandlingsflyt.flate.behandling.AvklaringsbehovDTO(
                                            behov.definisjon,
                                            behov.status(),
                                            emptyList()
                                        )
                                    },
                                no.nav.aap.behandlingsflyt.flate.behandling.hentUtRelevantVilkårForSteg(
                                    behandling.vilkårsresultat(),
                                    stegType
                                )
                            )
                        }, behandling.aktivtSteg().tilstand.steg()
                    )
                )
            }
        }
        route("/{referanse}/flyt-2") {
            get<no.nav.aap.behandlingsflyt.flate.behandling.BehandlingReferanse, no.nav.aap.behandlingsflyt.flate.behandling.BehandlingFlytOgTilstand2Dto> { req ->
                val behandling = no.nav.aap.behandlingsflyt.flate.behandling.behandling(req)
                val stegGrupper = LinkedHashMap<StegGruppe, LinkedList<StegType>>()
                for (steg in behandling.flyt().stegene()) {
                    val gruppe = stegGrupper.getOrDefault(steg.gruppe, LinkedList<StegType>())
                    gruppe.add(steg)
                    stegGrupper[steg.gruppe] = gruppe
                }

                respond(
                    no.nav.aap.behandlingsflyt.flate.behandling.BehandlingFlytOgTilstand2Dto(
                        stegGrupper.map { gruppe ->
                            no.nav.aap.behandlingsflyt.flate.behandling.FlytGruppe(
                                gruppe.key,
                                gruppe.value.map { stegType ->
                                    no.nav.aap.behandlingsflyt.flate.behandling.FlytSteg(stegType,
                                        behandling.avklaringsbehov()
                                            .filter { avklaringsbehov -> avklaringsbehov.skalLøsesISteg(stegType) }
                                            .map { behov ->
                                                no.nav.aap.behandlingsflyt.flate.behandling.AvklaringsbehovDTO(
                                                    behov.definisjon,
                                                    behov.status(),
                                                    emptyList()
                                                )
                                            },
                                        no.nav.aap.behandlingsflyt.flate.behandling.hentUtRelevantVilkårForSteg(
                                            behandling.vilkårsresultat(),
                                            stegType
                                        )
                                    )
                                }
                            )
                        }, behandling.aktivtSteg().tilstand.steg(),
                        behandling.aktivtSteg().tilstand.steg().gruppe
                    )
                )
            }
        }
    }
}

private fun behandling(req: no.nav.aap.behandlingsflyt.flate.behandling.BehandlingReferanse): Behandling {
    val eksternReferanse: UUID
    try {
        eksternReferanse = req.ref()
    } catch (exception: IllegalArgumentException) {
        throw no.nav.aap.behandlingsflyt.domene.ElementNotFoundException()
    }

    val behandling = BehandlingTjeneste.hent(eksternReferanse)
    return behandling
}