package no.nav.aap.behandlingsflyt.flyt

import com.papsign.ktor.openapigen.route.TagModule
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.response.respondWithStatus
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.*
import no.nav.aap.behandlingsflyt.Tags
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.FrivilligeAvklaringsbehov
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.flyt.flate.VilkårDTO
import no.nav.aap.behandlingsflyt.flyt.flate.VilkårsperiodeDTO
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.pip.PipRepository
import no.nav.aap.behandlingsflyt.prosessering.ProsesserBehandlingJobbUtfører
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.behandlingsflyt.sakogbehandling.lås.TaSkriveLåsRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.PdlPersoninfoBulkGateway
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import no.nav.aap.repository.RepositoryProvider
import no.nav.aap.tilgang.AuthorizationParamPathConfig
import no.nav.aap.tilgang.BehandlingPathParam
import no.nav.aap.tilgang.authorizedGet
import javax.sql.DataSource

fun NormalOpenAPIRoute.behandlingApi(dataSource: DataSource) {
    route("/api/behandling") {
        route("/{referanse}") {
            get<BehandlingReferanse, DetaljertBehandlingDTO>(TagModule(listOf(Tags.Behandling))) { req ->
                val dto = dataSource.transaction(readOnly = true) { connection ->
                    val repositoryProvider = RepositoryProvider(connection)
                    val behandlingRepository = repositoryProvider.provide(BehandlingRepository::class)
                    val avklaringsbehovRepository = repositoryProvider.provide(AvklaringsbehovRepository::class)
                    val vilkårsresultatRepository = repositoryProvider.provide(VilkårsresultatRepository::class)

                    val behandling = behandling(behandlingRepository, req)
                    val flyt = utledType(behandling.typeBehandling()).flyt()
                    DetaljertBehandlingDTO(
                        referanse = behandling.referanse.referanse,
                        type = behandling.typeBehandling().name,
                        status = behandling.status(),
                        opprettet = behandling.opprettetTidspunkt,
                        skalForberede = behandling.harIkkeVærtAktivitetIDetSiste(),
                        avklaringsbehov = FrivilligeAvklaringsbehov(
                            avklaringsbehov(
                                avklaringsbehovRepository,
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
                        vilkår = vilkårResultat(vilkårsresultatRepository, behandling.id).alle().map { vilkår ->
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
            get<BehandlingReferanse, DetaljertBehandlingDTO>(TagModule(listOf(Tags.Behandling))) { req ->
                dataSource.transaction { connection ->
                    val repositoryProvider = RepositoryProvider(connection)
                    val taSkriveLåsRepository = repositoryProvider.provide(TaSkriveLåsRepository::class)
                    val behandlingRepository = repositoryProvider.provide(BehandlingRepository::class)
                    val lås = taSkriveLåsRepository.lås(req.referanse)
                    val behandling = behandling(behandlingRepository, req)
                    val flytJobbRepository = FlytJobbRepository(connection)
                    if (!behandling.status()
                            .erAvsluttet() && behandling.harIkkeVærtAktivitetIDetSiste() && flytJobbRepository.hentJobberForBehandling(
                            behandling.id.toLong()
                        ).isEmpty()
                    ) {
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
                    val repositoryProvider = RepositoryProvider(connection)
                    val pipRepository = repositoryProvider.provide(PipRepository::class)
                    pipRepository.finnIdenterPåBehandling(BehandlingReferanse(referanse))
                }

                val response = HashMap<String, String>()
                val personInfoListe = PdlPersoninfoBulkGateway.hentPersoninfoForIdenter(identer.map { Ident(it.ident) })
                personInfoListe.forEach { personinfo ->
                    response[personinfo.ident.identifikator] = personinfo.fulltNavn()
                }

                respond(
                    BehandlingPersoninfo(response)
                )
            }
        }
    }
}

private fun behandling(behandlingRepository: BehandlingRepository, req: BehandlingReferanse): Behandling {
    return BehandlingReferanseService(behandlingRepository).behandling(req)
}

private fun avklaringsbehov(
    avklaringsbehovRepository: AvklaringsbehovRepository,
    behandlingId: BehandlingId
): Avklaringsbehovene {
    return avklaringsbehovRepository.hentAvklaringsbehovene(behandlingId)
}

private fun vilkårResultat(
    vilkårsResultatRepository: VilkårsresultatRepository,
    behandlingId: BehandlingId
): Vilkårsresultat {
    return vilkårsResultatRepository.hent(behandlingId)
}