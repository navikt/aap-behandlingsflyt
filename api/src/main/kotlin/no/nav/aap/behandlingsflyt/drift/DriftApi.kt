package no.nav.aap.behandlingsflyt.drift

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.response.respondWithStatus
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.*
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingReferanse
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Innvilgelsesårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Status
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.flate.SaksnummerParameter
import no.nav.aap.behandlingsflyt.tilgang.relevanteIdenterForBehandlingResolver
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.httpklient.exception.VerdiIkkeFunnetException
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.komponenter.server.auth.bruker
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.tilgang.AuthorizationParamPathConfig
import no.nav.aap.tilgang.BehandlingPathParam
import no.nav.aap.tilgang.Operasjon
import no.nav.aap.tilgang.SakPathParam
import no.nav.aap.tilgang.authorizedPost
import no.nav.aap.tilgang.plugin.kontrakt.BehandlingreferanseResolver
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*
import javax.sql.DataSource
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status as BehandlingStatus
import no.nav.aap.behandlingsflyt.kontrakt.sak.Status as SakStatus

/**
 * API for å utføre manuelle operasjoner med forsøk på å rette opp i låste saker av varierende grunn.
 * Ikke bruk dette ved mindre du vet hva du gjør.
 * Med tid kan vi ha et admin-verktøy for alle disse.
 * */

private val log = LoggerFactory.getLogger("driftApi")
fun NormalOpenAPIRoute.driftApi(
    dataSource: DataSource,
    repositoryRegistry: RepositoryRegistry,
    gatewayProvider: GatewayProvider,
) {
    route("/api/drift") {
        data class KjorFraSteg(val steg: StegType)
        route("/behandling/{referanse}/kjor-fra-steg") {
            authorizedPost<BehandlingReferanse, Unit, KjorFraSteg>(
                AuthorizationParamPathConfig(
                    behandlingPathParam = BehandlingPathParam("referanse"),
                    operasjon = Operasjon.DRIFTE
                )
            ) { params, request ->
                dataSource.transaction { connection ->
                    val repositoryProvider = repositoryRegistry.provider(connection)
                    val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                    val driftfunksjoner = Driftfunksjoner(repositoryProvider, gatewayProvider)

                    val behandling = behandlingRepository.hent(BehandlingReferanse(params.referanse))
                    driftfunksjoner.kjørFraSteg(behandling, request.steg)
                }
                respondWithStatus(HttpStatusCode.NoContent)
            }
        }

        data class AvbrytBrevBody(val begrunnelse: String)
        route("/brev/{brevbestillingReferanse}/avbryt") {
            authorizedPost<BrevbestillingReferanse, Unit, AvbrytBrevBody>(
                AuthorizationParamPathConfig(
                    operasjon = Operasjon.DRIFTE,
                    relevanteIdenterResolver = relevanteIdenterForBehandlingResolver(repositoryRegistry, dataSource),
                    behandlingPathParam = BehandlingPathParam(
                        "brevbestillingReferanse",
                        behandlingFraBrevbestilling(repositoryRegistry, dataSource)
                    )
                )
            ) { param, req ->
                dataSource.transaction { connection ->
                    val repositoryProvider = repositoryRegistry.provider(connection)
                    val driftsfunksjoner = Driftfunksjoner(repositoryProvider, gatewayProvider)

                    driftsfunksjoner.avbrytVedtsaksbrevBestilling(bruker(), param, req.begrunnelse)

                    log.info("Brevbestilling med referanse ${param} er avbrutt av ${bruker()}.")
                }
                respondWithStatus(HttpStatusCode.NoContent)
            }
        }

        route("/behandling/{referanse}/vilkår") {
            authorizedPost<BehandlingReferanse, List<VilkårDriftsinfoDTO>, Unit>(
                AuthorizationParamPathConfig(
                    behandlingPathParam = BehandlingPathParam("referanse"),
                    operasjon = Operasjon.DRIFTE
                )
            ) { params, _ ->
                val vilkår = dataSource.transaction { connection ->
                    val repositoryProvider = repositoryRegistry.provider(connection)
                    val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()

                    val vilkårRepository = repositoryProvider.provide<VilkårsresultatRepository>()

                    val behandling = behandlingRepository.hent(BehandlingReferanse(params.referanse))

                    vilkårRepository.hent(behandling.id)
                        .alle()
                        .map { vilkår ->
                            VilkårDriftsinfoDTO(
                                vilkår.type,
                                perioder = vilkår.vilkårsperioder().map { vp ->
                                    ForenkletVilkårsperiode(
                                        vp.periode,
                                        vp.utfall,
                                        vp.manuellVurdering,
                                        vp.avslagsårsak,
                                        vp.innvilgelsesårsak
                                    )
                                },
                                vurdertTidspunkt = vilkår.vurdertTidspunkt
                            )
                        }
                        .sortedBy { it.vurdertTidspunkt }
                }

                respond(vilkår)
            }
        }

        route("/sak/{saksnummer}/info") {
            authorizedPost<SaksnummerParameter, SakDriftsinfoDTO, Unit>(
                AuthorizationParamPathConfig(
                    sakPathParam = SakPathParam("saksnummer"),
                    operasjon = Operasjon.DRIFTE,
                ),
            ) { params, _ ->
                val sakDriftsinfoDTO = dataSource.transaction { connection ->
                    val repositoryProvider = repositoryRegistry.provider(connection)

                    val sakRepository = repositoryProvider.provide<SakRepository>()
                    val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                    val avklaringsbehovRepository = repositoryProvider.provide<AvklaringsbehovRepository>()

                    val sak = sakRepository.hentHvisFinnes(Saksnummer(params.saksnummer))
                        ?: throw VerdiIkkeFunnetException("Sak med saksnummer ${params.saksnummer} finnes ikke")

                    val behandlinger = behandlingRepository.hentAlleFor(sak.id)
                        .map { behandling ->
                            val avklaringsbehovene = avklaringsbehovRepository
                                .hentAvklaringsbehovene(behandling.id)
                                .alle()
                                .flatMap { avklaringsbehov ->
                                    avklaringsbehov.historikk.map { endring ->
                                        ForenkletAvklaringsbehov(
                                            definisjon = avklaringsbehov.definisjon,
                                            status = endring.status,
                                            tidsstempel = endring.tidsstempel,
                                            endretAv = endring.endretAv
                                        )
                                    }
                                }.sortedByDescending { it.tidsstempel }

                            BehandlingDriftsinfo.fra(behandling, avklaringsbehovene)
                        }

                    SakDriftsinfoDTO(
                        saksnummer = sak.saksnummer.toString(),
                        status = sak.status(),
                        rettighetsperiode = sak.rettighetsperiode,
                        opprettetTidspunkt = sak.opprettetTidspunkt,
                        behandlinger = behandlinger,
                    )
                }

                require(!Regex("""\d{11}""").containsMatchIn(DefaultJsonMapper.toJson(sakDriftsinfoDTO))) {
                    "DTO-en inneholder (potensielt) sensitive personopplysninger!"
                }

                respond(sakDriftsinfoDTO)
            }
        }
    }
}

private data class SakDriftsinfoDTO(
    val saksnummer: String,
    val status: SakStatus,
    val rettighetsperiode: Periode,
    val opprettetTidspunkt: LocalDateTime = LocalDateTime.now(),
    val behandlinger: List<BehandlingDriftsinfo>,
)

private data class BehandlingDriftsinfo(
    val referanse: UUID,
    val type: String,
    val status: BehandlingStatus,
    val vurderingsbehov: List<Vurderingsbehov>,
    val årsakTilOpprettelse: ÅrsakTilOpprettelse?,
    val opprettet: LocalDateTime,
    val avklaringsbehov: List<ForenkletAvklaringsbehov>,
) {
    companion object {
        fun fra(behandling: Behandling, avklaringsbehovene: List<ForenkletAvklaringsbehov>) =
            BehandlingDriftsinfo(
                referanse = behandling.referanse.referanse,
                type = behandling.typeBehandling().identifikator(),
                status = behandling.status(),
                vurderingsbehov = behandling.vurderingsbehov().map(VurderingsbehovMedPeriode::type).distinct(),
                årsakTilOpprettelse = behandling.årsakTilOpprettelse,
                opprettet = behandling.opprettetTidspunkt,
                avklaringsbehov = avklaringsbehovene,
            )
    }
}

private data class ForenkletAvklaringsbehov(
    val definisjon: Definisjon,
    val status: Status,
    val tidsstempel: LocalDateTime = LocalDateTime.now(),
    val endretAv: String
)

private data class VilkårDriftsinfoDTO(
    val type: Vilkårtype,
    val perioder: List<ForenkletVilkårsperiode>,
    val vurdertTidspunkt: LocalDateTime?,
)

private data class ForenkletVilkårsperiode(
    val periode: Periode,
    val utfall: Utfall,
    val manuellVurdering: Boolean,
    val avslagsårsak: Avslagsårsak?,
    val innvilgelsesårsak: Innvilgelsesårsak?
)

fun behandlingFraBrevbestilling(
    repositoryRegistry: RepositoryRegistry,
    dataSource: DataSource
): BehandlingreferanseResolver {
    return BehandlingreferanseResolver { referanse ->
        dataSource.transaction(readOnly = true) {
            repositoryRegistry.provider(it).provide(BrevbestillingRepository::class)
                .hentBehandlingsreferanseForBestilling(BrevbestillingReferanse(UUID.fromString(referanse))).referanse
        }
    }
}
