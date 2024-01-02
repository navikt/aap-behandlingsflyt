package no.nav.aap.behandlingsflyt.flyt.flate

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import com.zaxxer.hikari.HikariDataSource
import no.nav.aap.behandlingsflyt.behandling.Behandling
import no.nav.aap.behandlingsflyt.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.avklaringsbehov.AvklaringsbehovRepositoryImpl
import no.nav.aap.behandlingsflyt.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.avklaringsbehov.FrivilligeAvklaringsbehov
import no.nav.aap.behandlingsflyt.behandling.flate.AvklaringsbehovDTO
import no.nav.aap.behandlingsflyt.behandling.flate.BehandlingReferanse
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.dbconnect.transaction
import no.nav.aap.behandlingsflyt.behandling.flate.BehandlingReferanseService
import no.nav.aap.behandlingsflyt.flyt.steg.StegGruppe
import no.nav.aap.behandlingsflyt.flyt.steg.StegType
import no.nav.aap.behandlingsflyt.flyt.vilkår.Vilkår
import no.nav.aap.behandlingsflyt.flyt.vilkår.Vilkårsresultat
import no.nav.aap.behandlingsflyt.flyt.vilkår.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.flyt.vilkår.Vilkårtype

fun NormalOpenAPIRoute.flytApi(dataSource: HikariDataSource) {
    route("/api/behandling") {
        route("/{referanse}/flyt") {
            get<BehandlingReferanse, BehandlingFlytOgTilstandDto> { req ->
                val dto = dataSource.transaction { connection ->
                    val behandling = behandling(connection, req)
                    val stegGrupper: Map<StegGruppe, List<StegType>> =
                        behandling.flyt().stegene().groupBy { steg -> steg.gruppe }

                    val aktivtSteg = behandling.aktivtSteg()
                    var erFullført = true
                    val alleAvklaringsbehovInkludertFrivillige = FrivilligeAvklaringsbehov(
                        avklaringsbehov(
                            connection,
                            behandling.id
                        ),
                        behandling.flyt()
                    )
                    BehandlingFlytOgTilstandDto(
                        flyt = stegGrupper.map { (gruppe, steg) ->
                            erFullført = erFullført && gruppe != aktivtSteg.gruppe
                            FlytGruppe(
                                stegGruppe = gruppe,
                                erFullført = erFullført,
                                steg = steg.map { stegType ->
                                    FlytSteg(
                                        stegType = stegType,
                                        avklaringsbehov = alleAvklaringsbehovInkludertFrivillige.alle()
                                            .filter { avklaringsbehov -> avklaringsbehov.skalLøsesISteg(stegType) }
                                            .map { behov ->
                                                AvklaringsbehovDTO(
                                                    behov.definisjon,
                                                    behov.status(),
                                                    emptyList()
                                                )
                                            },
                                        vilkårDTO = hentUtRelevantVilkårForSteg(
                                            vilkårResultat(connection, behandling.id),
                                            stegType
                                        )
                                    )
                                }
                            )
                        },
                        aktivtSteg = aktivtSteg,
                        aktivGruppe = aktivtSteg.gruppe,
                        behandlingVersjon = behandling.versjon
                    )
                }
                respond(dto)
            }
        }
    }
}

private fun behandling(connection: DBConnection, req: BehandlingReferanse): Behandling {
    return BehandlingReferanseService(connection).behandling(req)
}

private fun avklaringsbehov(connection: DBConnection, behandlingId: BehandlingId): Avklaringsbehovene {
    return AvklaringsbehovRepositoryImpl(connection).hentAvklaringsbehovene(behandlingId)
}

private fun vilkårResultat(connection: DBConnection, behandlingId: BehandlingId): Vilkårsresultat {
    return VilkårsresultatRepository(connection).hent(behandlingId)
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
