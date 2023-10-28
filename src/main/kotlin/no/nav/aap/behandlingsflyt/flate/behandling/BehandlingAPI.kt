package no.nav.aap.behandlingsflyt.flate.behandling

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import com.zaxxer.hikari.HikariDataSource
import no.nav.aap.behandlingsflyt.behandling.Behandling
import no.nav.aap.behandlingsflyt.dbstuff.transaction
import no.nav.aap.behandlingsflyt.faktagrunnlag.BehandlingReferanseService
import no.nav.aap.behandlingsflyt.flyt.steg.StegGruppe
import no.nav.aap.behandlingsflyt.flyt.steg.StegType
import no.nav.aap.behandlingsflyt.flyt.vilkår.Vilkår
import no.nav.aap.behandlingsflyt.flyt.vilkår.Vilkårsresultat
import no.nav.aap.behandlingsflyt.flyt.vilkår.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.flyt.vilkår.Vilkårtype

fun NormalOpenAPIRoute.behandlingApi(dataSource: HikariDataSource) {
    route("/api/behandling") {
        route("/{referanse}") {
            get<BehandlingReferanse, DetaljertBehandlingDTO> { req ->
                val behandling = behandling(dataSource, req)

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
                    vilkår = VilkårsresultatRepository.hent(behandling.id).alle().map { vilkår ->
                        VilkårDTO(
                            vilkårtype = vilkår.type,
                            perioder = vilkår.vilkårsperioder()
                                .map { vp ->
                                    VilkårsperiodeDTO(
                                        periode = vp.periode,
                                        utfall = vp.utfall,
                                        manuellVurdering = vp.manuellVurdering,
                                        begrunnelse = vp.begrunnelse,
                                        avslagsårsak = vp.avslagsårsak,
                                        innvilgelsesårsak = vp.innvilgelsesårsak
                                    )
                                })
                    },
                    aktivtSteg = behandling.stegHistorikk().last().tilstand.steg()
                )

                respond(dto)
            }
        }
        route("/{referanse}/flyt") {
            get<BehandlingReferanse, BehandlingFlytOgTilstandDto> { req ->
                val behandling = behandling(dataSource, req)

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
                                    VilkårsresultatRepository.hent(behandling.id),
                                    stegType
                                )
                            )
                        }, behandling.aktivtSteg()
                    )
                )
            }
        }
        route("/{referanse}/flyt-2") {
            get<BehandlingReferanse, BehandlingFlytOgTilstand2Dto> { req ->
                val behandling = behandling(dataSource, req)
                val stegGrupper: Map<StegGruppe, List<StegType>> =
                    behandling.flyt().stegene().groupBy { steg -> steg.gruppe }

                val aktivtSteg = behandling.aktivtSteg()
                var erFullført = true
                respond(
                    BehandlingFlytOgTilstand2Dto(
                        flyt = stegGrupper.map { (gruppe, steg) ->
                            erFullført = erFullført && gruppe != aktivtSteg.gruppe
                            FlytGruppe(
                                stegGruppe = gruppe,
                                erFullført = erFullført,
                                steg = steg.map { stegType ->
                                    FlytSteg(
                                        stegType = stegType,
                                        avklaringsbehov = behandling.avklaringsbehov()
                                            .filter { avklaringsbehov -> avklaringsbehov.skalLøsesISteg(stegType) }
                                            .map { behov ->
                                                AvklaringsbehovDTO(
                                                    behov.definisjon,
                                                    behov.status(),
                                                    emptyList()
                                                )
                                            },
                                        vilkårDTO = hentUtRelevantVilkårForSteg(
                                            VilkårsresultatRepository.hent(behandling.id),
                                            stegType
                                        )
                                    )
                                }
                            )
                        },
                        aktivtSteg = aktivtSteg,
                        aktivGruppe = aktivtSteg.gruppe
                    )
                )
            }
        }
    }
}

private fun behandling(dataSource: HikariDataSource, req: BehandlingReferanse): Behandling {
    var behandling: Behandling? = null
    dataSource.transaction {
        behandling = BehandlingReferanseService(it).behandling(req)
    }
    return behandling!!
}

private fun hentUtRelevantVilkårForSteg(vilkårsresultat: Vilkårsresultat, stegType: StegType): VilkårDTO? {
    var vilkår: Vilkår? = null
    if (stegType == StegType.AVKLAR_SYKDOM) {
        vilkår = vilkårsresultat.finnVilkår(Vilkårtype.SYKDOMSVILKÅRET)
    }
    if (stegType == StegType.VURDER_ALDER) {
        vilkår = vilkårsresultat.finnVilkår(Vilkårtype.ALDERSVILKÅRET)
    }
    if (stegType == StegType.VURDER_BISTANDSBEHOV) {
        vilkår = vilkårsresultat.finnVilkår(Vilkårtype.BISTANDSVILKÅRET)
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
                vp.avslagsårsak,
                vp.innvilgelsesårsak
            )
        })
}
