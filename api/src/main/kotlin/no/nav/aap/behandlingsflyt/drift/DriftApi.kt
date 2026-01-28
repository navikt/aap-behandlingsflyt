package no.nav.aap.behandlingsflyt.drift

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.response.respondWithStatus
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.*
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Status
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.flate.BehandlinginfoDTO
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.tilgang.AuthorizationParamPathConfig
import no.nav.aap.tilgang.BehandlingPathParam
import no.nav.aap.tilgang.Operasjon
import no.nav.aap.tilgang.authorizedPost
import java.time.LocalDateTime
import javax.sql.DataSource

/**
 * API for å utføre manuelle operasjoner med forsøk på å rette opp i låste saker av varierende grunn.
 * Ikke bruk dette ved mindre du vet hva du gjør.
 * Med tid kan vi ha et admin-verktøy for alle disse.
 * */

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

        route("/behandling/{referanse}/info") {
            authorizedPost<BehandlingReferanse, BehandlingDriftsinfoDTO, Unit>(
                AuthorizationParamPathConfig(
                    behandlingPathParam = BehandlingPathParam("referanse"),
                    operasjon = Operasjon.DRIFTE
                )
            ) { params, _ ->
                val behandlingDriftsinfo = dataSource.transaction { connection ->
                    val repositoryProvider = repositoryRegistry.provider(connection)
                    val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()

                    val behandling = behandlingRepository.hent(BehandlingReferanse(params.referanse))

                    val avklaringsbehovene = repositoryProvider.provide<AvklaringsbehovRepository>()
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

                    BehandlingDriftsinfoDTO(
                        behandling = behandling.tilDTO(),
                        avklaringsbehov = avklaringsbehovene,
                    )
                }

                require(!Regex("""\d{11}""").containsMatchIn(DefaultJsonMapper.toJson(behandlingDriftsinfo))) {
                    "Behandlingen inneholder (potensielt) sensitive personopplysninger!"
                }

                respond(behandlingDriftsinfo)
            }
        }
    }
}

private data class BehandlingDriftsinfoDTO(
    val behandling: BehandlinginfoDTO,
    val avklaringsbehov: List<ForenkletAvklaringsbehov>,
)

private fun Behandling.tilDTO() = BehandlinginfoDTO(
    referanse = this.referanse.referanse,
    type = this.typeBehandling().identifikator(),
    status = this.status(),
    vurderingsbehov = this.vurderingsbehov().map(VurderingsbehovMedPeriode::type),
    årsakTilOpprettelse = this.årsakTilOpprettelse,
    opprettet = this.opprettetTidspunkt,
    eksternSaksbehandlingsløsningUrl = null,
)

private data class ForenkletAvklaringsbehov(
    val definisjon: Definisjon,
    val status: Status,
    val tidsstempel: LocalDateTime = LocalDateTime.now(),
    val endretAv: String
)
