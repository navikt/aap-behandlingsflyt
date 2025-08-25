package no.nav.aap.behandlingsflyt.flyt

import com.papsign.ktor.openapigen.route.TagModule
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.response.respondWithStatus
import com.papsign.ktor.openapigen.route.route
import com.papsign.ktor.openapigen.route.tag
import io.ktor.http.*
import no.nav.aap.behandlingsflyt.Tags
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.FrivilligeAvklaringsbehov
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.VirkningstidspunktUtleder
import no.nav.aap.behandlingsflyt.behandling.vedtak.VedtakService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.dokument.KlagedokumentInformasjonUtleder
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.pip.PipRepository
import no.nav.aap.behandlingsflyt.prosessering.ProsesserBehandlingService
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.behandlingsflyt.sakogbehandling.lås.TaSkriveLåsRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersoninfoBulkGateway
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.tilgang.AuthorizationParamPathConfig
import no.nav.aap.tilgang.BehandlingPathParam
import no.nav.aap.tilgang.authorizedGet
import org.slf4j.LoggerFactory
import java.time.LocalDate
import javax.sql.DataSource

private val log = LoggerFactory.getLogger("behandlingApi")

fun NormalOpenAPIRoute.behandlingApi(
    dataSource: DataSource,
    repositoryRegistry: RepositoryRegistry,
    gatewayProvider: GatewayProvider,
) {
    route("/api/behandling").tag(Tags.Behandling) {
        route("/{referanse}") {
            authorizedGet<BehandlingReferanse, DetaljertBehandlingDTO>(
                AuthorizationParamPathConfig(
                    behandlingPathParam = BehandlingPathParam(
                        "referanse"
                    )
                )
            ) { req ->
                val dto = dataSource.transaction(readOnly = true) { connection ->
                    val repositoryProvider = repositoryRegistry.provider(connection)
                    val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                    val avklaringsbehovRepository =
                        repositoryProvider.provide<AvklaringsbehovRepository>()
                    val vilkårsresultatRepository =
                        repositoryProvider.provide<VilkårsresultatRepository>()

                    val behandling = behandling(behandlingRepository, req)
                    val virkningstidspunkt =
                        runCatching {
                            if (behandling.erYtelsesbehandling()) VirkningstidspunktUtleder(
                                vilkårsresultatRepository = vilkårsresultatRepository
                            ).utledVirkningsTidspunkt(
                                behandling.id
                            ) else null
                        }.getOrElse {
                            log.warn("Feil ved utleding av virkningstidspunkt for behandling ${behandling.id}", it)
                            null
                        }
                    val flyt = behandling.flyt()

                    val kravMottatt = finnKravMottatt(repositoryProvider, behandling)
                    val tilhørendeKlagebehandling = tilhørendeKlagebehandling(
                        repositoryProvider,
                        behandling
                    )

                    val vurderingsbehovOgÅrsaker = behandlingRepository.hentVurderingsbehovOgÅrsaker(behandling.id)

                    DetaljertBehandlingDTO(
                        referanse = behandling.referanse.referanse,
                        type = behandling.typeBehandling(),
                        status = behandling.status(),
                        opprettet = behandling.opprettetTidspunkt,
                        skalForberede = behandling.harIkkeVærtAktivitetIDetSiste() && !behandling.status()
                            .erAvsluttet(),
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
                        vilkår = vilkårResultat(vilkårsresultatRepository, behandling.id).alle()
                            .map { vilkår ->
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
                                        },
                                    vurdertDato = vilkår.vurdertTidspunkt?.toLocalDate())
                            },
                        aktivtSteg = behandling.aktivtSteg(),
                        versjon = behandling.versjon,
                        virkningstidspunkt = virkningstidspunkt,
                        kravMottatt = kravMottatt,
                        tilhørendeKlagebehandling = tilhørendeKlagebehandling?.referanse,
                        vedtaksdato = VedtakService(repositoryProvider).vedtakstidspunkt(behandling)?.toLocalDate(),
                        vurderingsbehovOgÅrsaker = vurderingsbehovOgÅrsaker
                    )
                }
                respond(dto)
            }
        }
        route("/{referanse}/forbered") {
            authorizedGet<BehandlingReferanse, DetaljertBehandlingDTO>(
                AuthorizationParamPathConfig(
                    behandlingPathParam = BehandlingPathParam(
                        "referanse"
                    )
                )
            ) { req ->
                dataSource.transaction { connection ->
                    val repositoryProvider = repositoryRegistry.provider(connection)
                    val behandling = behandling(repositoryProvider.provide(), req)
                    if (behandling.status().erAvsluttet()) {
                        return@transaction
                    }
                    val taSkriveLåsRepository = repositoryProvider.provide<TaSkriveLåsRepository>()
                    val lås = taSkriveLåsRepository.lås(req.referanse)
                    if (!behandling.status().erAvsluttet()
                        && behandling.harIkkeVærtAktivitetIDetSiste()
                    ) {
                        ProsesserBehandlingService(repositoryProvider, gatewayProvider).triggProsesserBehandling(
                            behandling.sakId,
                            behandling.id
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
                ),
                null, TagModule(listOf(Tags.Behandling))
            ) { req ->

                val referanse = req.referanse

                val identer = dataSource.transaction(readOnly = true) { connection ->
                    val repositoryProvider = repositoryRegistry.provider(connection)
                    val pipRepository = repositoryProvider.provide<PipRepository>()
                    pipRepository.finnIdenterPåBehandling(BehandlingReferanse(referanse))
                }

                val response = HashMap<String, String>()
                val personInfoListe = gatewayProvider.provide(PersoninfoBulkGateway::class)
                    .hentPersoninfoForIdenter(identer.map { Ident(it.ident) })
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

private fun finnKravMottatt(
    repositoryProvider: RepositoryProvider,
    behandling: Behandling
): LocalDate? {
    if (behandling.typeBehandling() != TypeBehandling.Klage) return null
    return KlagedokumentInformasjonUtleder(repositoryProvider)
        .utledKravMottattDatoForKlageBehandling(behandling.id)
}

private fun tilhørendeKlagebehandling(
    repositoryProvider: RepositoryProvider,
    behandling: Behandling
): BehandlingReferanse? {
    if (behandling.typeBehandling() != TypeBehandling.SvarFraAndreinstans) return null
    return KlagedokumentInformasjonUtleder(repositoryProvider)
        .utledKlagebehandlingForSvar(behandling.id)
}