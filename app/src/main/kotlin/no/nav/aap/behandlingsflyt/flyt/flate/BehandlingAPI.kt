package no.nav.aap.behandlingsflyt.flyt.flate

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.response.respondWithStatus
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.*
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepositoryImpl
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.FrivilligeAvklaringsbehov
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.flyt.utledType
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.behandlingsflyt.sakogbehandling.lås.TaSkriveLåsRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.PdlPersoninfoBulkGateway
import no.nav.aap.behandlingsflyt.server.prosessering.ProsesserBehandlingJobbUtfører
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import no.nav.aap.pip.PipRepository
import no.nav.aap.tilgang.AuthorizationParamPathConfig
import no.nav.aap.tilgang.BehandlingPathParam
import no.nav.aap.tilgang.authorizedGet
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import no.nav.aap.verdityper.sakogbehandling.Ident
import javax.sql.DataSource

fun NormalOpenAPIRoute.behandlingApi(dataSource: DataSource) {
    route("/api/behandling") {
        route("/{referanse}") {
            get<BehandlingReferanse, DetaljertBehandlingDTO> { req ->
                val dto = dataSource.transaction(readOnly = true) { connection ->
                    val behandling = behandling(connection, req)
                    val flyt = utledType(behandling.typeBehandling()).flyt()
                    DetaljertBehandlingDTO(
                        referanse = behandling.referanse.referanse,
                        type = behandling.typeBehandling().name,
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
                                        begrunnelse = endring.begrunnelse,
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
                    val lås = taSkriveLåsRepository.lås(req.referanse)
                    val behandling = behandling(connection, req)
                    val flytJobbRepository = FlytJobbRepository(connection)
                    if (!behandling.status().erAvsluttet() &&  flytJobbRepository.hentJobberForBehandling(behandling.id.toLong()).isEmpty()) {
                        flytJobbRepository.leggTil(
                            JobbInput(ProsesserBehandlingJobbUtfører).forBehandling(
                                behandling.sakId.toLong(),
                                behandling.id.toLong()
                            )
                        )
                    }
                    taSkriveLåsRepository.verifiserSkrivelås(lås)
                }
                respondWithStatus(HttpStatusCode.Accepted)
            }
        }
        route("/{referanse}/personinformasjon") {
            authorizedGet<BehandlingReferanse, BehandlingPersoninfo>(
                AuthorizationParamPathConfig(
                    behandlingPathParam = BehandlingPathParam("referanse")
                )
            ) { req ->

                val referanse = req.referanse

                val identer = dataSource.transaction(readOnly = true) { connection ->
                    PipRepository(connection).finnIdenterPåBehandling(BehandlingReferanse(referanse))
                }

                val response = HashMap<String, String>()
                val personinfo = PdlPersoninfoBulkGateway.hentPersoninfoForIdenter(identer.map { Ident(it.ident) })
                personinfo.forEach { personinfo -> response[personinfo.ident.identifikator] = personinfo.fulltNavn() }

                respond(
                    BehandlingPersoninfo(response)
                )
            }
        }
    }
}

private fun behandling(connection: DBConnection, req: BehandlingReferanse): Behandling {
    return BehandlingReferanseService(BehandlingRepositoryImpl(connection)).behandling(req)
}

private fun avklaringsbehov(connection: DBConnection, behandlingId: BehandlingId): Avklaringsbehovene {
    return AvklaringsbehovRepositoryImpl(connection).hentAvklaringsbehovene(behandlingId)
}

private fun vilkårResultat(connection: DBConnection, behandlingId: BehandlingId): Vilkårsresultat {
    return VilkårsresultatRepository(connection).hent(behandlingId)
}