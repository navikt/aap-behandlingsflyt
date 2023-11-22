package no.nav.aap.behandlingsflyt.behandling.flate

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import com.zaxxer.hikari.HikariDataSource
import no.nav.aap.behandlingsflyt.behandling.Behandling
import no.nav.aap.behandlingsflyt.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.dbconnect.transaction
import no.nav.aap.behandlingsflyt.faktagrunnlag.BehandlingReferanseService
import no.nav.aap.behandlingsflyt.flyt.flate.VilkårDTO
import no.nav.aap.behandlingsflyt.flyt.flate.VilkårsperiodeDTO
import no.nav.aap.behandlingsflyt.flyt.vilkår.Vilkårsresultat
import no.nav.aap.behandlingsflyt.flyt.vilkår.VilkårsresultatRepository

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

                    avklaringsbehov = avklaringsbehov(dataSource, behandling.id).alle().map { avklaringsbehov ->
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
                    vilkår = vilkårResultat(dataSource, behandling.id).alle().map { vilkår ->
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
                    aktivtSteg = behandling.stegHistorikk().last().tilstand.steg(),
                    versjon = behandling.versjon
                )

                respond(dto)
            }
        }
    }
}

private fun behandling(dataSource: HikariDataSource, req: BehandlingReferanse): Behandling {
    return dataSource.transaction {
        BehandlingReferanseService(it).behandling(req)
    }
}

private fun avklaringsbehov(dataSource: HikariDataSource, behandlingId: BehandlingId): Avklaringsbehovene {
    return dataSource.transaction {
        AvklaringsbehovRepository(it).hent(behandlingId)
    }
}

private fun vilkårResultat(dataSource: HikariDataSource, behandlingId: BehandlingId): Vilkårsresultat {
    return dataSource.transaction {
        VilkårsresultatRepository(it).hent(behandlingId)
    }
}