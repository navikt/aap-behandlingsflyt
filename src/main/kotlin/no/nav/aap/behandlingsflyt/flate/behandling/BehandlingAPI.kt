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
import no.nav.aap.behandlingsflyt.grunnlag.sykdom.SykdomsTjeneste
import no.nav.aap.behandlingsflyt.grunnlag.yrkesskade.YrkesskadeTjeneste
import no.nav.aap.behandlingsflyt.flyt.StegGruppe
import no.nav.aap.behandlingsflyt.flyt.StegType
import java.util.*

fun hentUtRelevantVilkårForSteg(vilkårsresultat: Vilkårsresultat, stegType: StegType): VilkårDTO? {
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
    return VilkårDTO(
        vilkår.type,
        perioder = vilkår.vilkårsperioder().map { vp ->
            VilkårsperiodeDTO(
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
            get<BehandlingReferanse, DetaljertBehandlingDTO> { req ->
                val behandling = behandling(req)

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
            get<BehandlingReferanse, SykdomsGrunnlagDto> { req ->
                val behandling = behandling(req)

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
        route("/{referanse}/grunnlag/medlemskap") {
            get<BehandlingReferanse, MedlemskapGrunnlagDto> { req ->
                val behandling = behandling(req)
                respond(MedlemskapGrunnlagDto())
            }
        }
        route("/{referanse}/flyt") {
            get<BehandlingReferanse, BehandlingFlytOgTilstandDto> { req ->
                val behandling = behandling(req)

                respond(
                    BehandlingFlytOgTilstandDto(
                        behandling.flyt().stegene().map { stegType ->
                            FlytSteg(
                                stegType,
                                behandling.avklaringsbehov()
                                    .filter { avklaringsbehov -> avklaringsbehov.skalLøsesISteg(stegType) }
                                    .map { behov ->
                                        AvklaringsbehovDTO(
                                            behov.definisjon,
                                            behov.status(),
                                            emptyList()
                                        )
                                    },
                                hentUtRelevantVilkårForSteg(
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
            get<BehandlingReferanse, BehandlingFlytOgTilstand2Dto> { req ->
                val behandling = behandling(req)
                val stegGrupper = LinkedHashMap<StegGruppe, LinkedList<StegType>>()
                for (steg in behandling.flyt().stegene()) {
                    val gruppe = stegGrupper.getOrDefault(steg.gruppe, LinkedList<StegType>())
                    gruppe.add(steg)
                    stegGrupper[steg.gruppe] = gruppe
                }

                respond(
                    BehandlingFlytOgTilstand2Dto(
                        stegGrupper.map { gruppe ->
                            FlytGruppe(
                                gruppe.key,
                                gruppe.value.map { stegType ->
                                    FlytSteg(stegType,
                                        behandling.avklaringsbehov()
                                            .filter { avklaringsbehov -> avklaringsbehov.skalLøsesISteg(stegType) }
                                            .map { behov ->
                                                AvklaringsbehovDTO(
                                                    behov.definisjon,
                                                    behov.status(),
                                                    emptyList()
                                                )
                                            },
                                        hentUtRelevantVilkårForSteg(
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