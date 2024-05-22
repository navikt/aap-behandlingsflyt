package no.nav.aap.behandlingsflyt.flyt.flate

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.*
import io.ktor.server.response.*
import no.nav.aap.behandlingsflyt.avklaringsbehov.AvklaringsbehovRepositoryImpl
import no.nav.aap.behandlingsflyt.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.avklaringsbehov.FrivilligeAvklaringsbehov
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.dbconnect.transaction
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.flyt.utledType
import no.nav.aap.behandlingsflyt.prosessering.ProsesserBehandlingOppgaveUtfører
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.behandlingsflyt.sakogbehandling.lås.TaSkriveLåsRepository
import no.nav.aap.motor.FlytOppgaveRepository
import no.nav.aap.motor.OppgaveInput
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import javax.sql.DataSource

fun NormalOpenAPIRoute.behandlingApi(dataSource: DataSource) {
    route("/api/behandling") {
        route("/{referanse}") {
            get<BehandlingReferanse, DetaljertBehandlingDTO> { req ->
                val dto = dataSource.transaction(readOnly = true) { connection ->
                    val behandling = behandling(connection, req)
                    val flyt = utledType(behandling.typeBehandling()).flyt()
                    DetaljertBehandlingDTO(
                        referanse = behandling.referanse,
                        type = behandling.typeBehandling().identifikator(),
                        status = behandling.status(),
                        opprettet = behandling.opprettetTidspunkt,

                        avklaringsbehov = FrivilligeAvklaringsbehov(
                            avklaringsbehov(
                                connection,
                                behandling.id
                            ),
                            flyt,
                            behandling.aktivtSteg()
                        ).alle().map { avklaringsbehov ->
                            AvklaringsbehovDTO(
                                definisjon = avklaringsbehov.definisjon,
                                status = avklaringsbehov.status(),
                                endringer = avklaringsbehov.historikk.map { endring ->
                                    EndringDTO(
                                        status = endring.status,
                                        tidsstempel = endring.tidsstempel,
                                        begrunnelse = endring.begrunnelse + "Noe",
                                        endretAv = endring.endretAv
                                    )
                                }
                            )
                        },
                        vilkår = vilkårResultat(connection, behandling.id).alle().map { vilkår ->
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
                        aktivtSteg = behandling.stegHistorikk().last().steg(),
                        versjon = behandling.versjon
                    )
                }
                respond(dto)
            }
        }
        route("/{referanse}/forbered") {
            get<BehandlingReferanse, DetaljertBehandlingDTO> { req ->
                dataSource.transaction { connection ->
                    val taSkriveLåsRepository = TaSkriveLåsRepository(connection)
                    val lås = taSkriveLåsRepository.lås(req.ref())
                    val behandling = behandling(connection, req)
                    val flytOppgaveRepository = FlytOppgaveRepository(connection)
                    if (flytOppgaveRepository.hentOppgaveForBehandling(behandling.id).isEmpty()) {
                        flytOppgaveRepository.leggTil(
                            OppgaveInput(ProsesserBehandlingOppgaveUtfører).forBehandling(
                                behandling.sakId,
                                behandling.id
                            )
                        )
                    }
                    taSkriveLåsRepository.verifiserSkrivelås(lås)
                }
                pipeline.context.respond(HttpStatusCode.Accepted)
                return@get
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