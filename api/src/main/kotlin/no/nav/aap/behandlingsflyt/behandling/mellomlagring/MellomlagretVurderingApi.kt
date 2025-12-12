package no.nav.aap.behandlingsflyt.behandling.mellomlagring

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.response.respondWithStatus
import com.papsign.ktor.openapigen.route.route
import com.papsign.ktor.openapigen.route.tag
import io.ktor.http.*
import no.nav.aap.behandlingsflyt.Tags
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.mdc.LogKontekst
import no.nav.aap.behandlingsflyt.mdc.LoggingKontekst
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.tilgang.relevanteIdenterForBehandlingResolver
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.komponenter.server.auth.bruker
import no.nav.aap.tilgang.AuthorizationBodyPathConfig
import no.nav.aap.tilgang.AuthorizationParamPathConfig
import no.nav.aap.tilgang.BehandlingPathParam
import no.nav.aap.tilgang.Operasjon
import no.nav.aap.tilgang.authorizedGet
import no.nav.aap.tilgang.authorizedPost
import java.time.LocalDateTime
import javax.sql.DataSource

fun NormalOpenAPIRoute.mellomlagretVurderingApi(dataSource: DataSource, repositoryRegistry: RepositoryRegistry) {
    route("/api/behandling").tag(Tags.Behandling) {
        route("/mellomlagret-vurdering") {
            authorizedPost<Unit, MellomlagretVurderingResponse, MellomlagretVurderingRequest>(
                AuthorizationBodyPathConfig(
                    operasjon = Operasjon.SAKSBEHANDLE,
                )
            ) { _, request ->
                val response = dataSource.transaction { connection ->
                    val repositoryProvider = repositoryRegistry.provider(connection)
                    val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                    val mellomlagretVurderingRepository = repositoryProvider.provide<MellomlagretVurderingRepository>()
                    val referanse = BehandlingReferanse(request.behandlingsReferanse)
                    val avklaringsbehovKode = AvklaringsbehovKode.valueOf(request.avklaringsbehovkode)
                    val behandling = behandlingRepository.hent(referanse)
                    LoggingKontekst(
                        repositoryProvider,
                        LogKontekst(referanse = referanse)
                    ).use {
                        if (behandling.status().erAvsluttet()) {
                            throw UgyldigForespørselException("Kan ikke mellomlagre vurderinger på en avsluttet behandling")
                        }

                        val mellomlagretVurdering = mellomlagretVurderingRepository.lagre(
                            MellomlagretVurdering(
                                behandlingId = behandling.id,
                                avklaringsbehovKode = avklaringsbehovKode,
                                data = request.data,
                                vurdertAv = bruker().ident,
                                vurdertDato = LocalDateTime.now()
                            )
                        )

                        MellomlagretVurderingResponse(
                            mellomlagretVurdering = mellomlagretVurdering.tilResponse()
                        )
                    }
                }
                respond(response)
            }
        }
        route("/mellomlagret-vurdering/{referanse}/{avklaringsbehovkode}") {
            authorizedGet<BehandlingReferanseMedAvklaringsbehov, MellomlagretVurderingResponse>(
                AuthorizationParamPathConfig(
                    relevanteIdenterResolver = relevanteIdenterForBehandlingResolver(repositoryRegistry, dataSource),
                    behandlingPathParam = BehandlingPathParam("referanse")
                )
            ) { params ->
                val behandlingsreferanse = BehandlingReferanse(params.referanse)
                val avklaringsbehovKode = AvklaringsbehovKode.valueOf(params.avklaringsbehovkode)
                val response = dataSource.transaction { connection ->
                    val repositoryProvider = repositoryRegistry.provider(connection)
                    val mellomlagretVurderingRepository = repositoryProvider.provide<MellomlagretVurderingRepository>()
                    val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                    val behandling = behandlingRepository.hent(behandlingsreferanse)
                    LoggingKontekst(
                        repositoryProvider,
                        LogKontekst(referanse = behandlingsreferanse)
                    ).use {
                        val mellomlagretVurdering = mellomlagretVurderingRepository.hentHvisEksisterer(
                            behandling.id,
                            avklaringsbehovKode
                        )
                        MellomlagretVurderingResponse(
                            mellomlagretVurdering = mellomlagretVurdering?.tilResponse(),
                        )
                    }
                }
                respond(response)
            }
        }

        route("/mellomlagret-vurdering/{referanse}/{avklaringsbehovkode}/slett") {
            authorizedPost<BehandlingReferanseMedAvklaringsbehov, Unit, Unit>(
                AuthorizationParamPathConfig(
                    relevanteIdenterResolver = relevanteIdenterForBehandlingResolver(repositoryRegistry, dataSource),
                    behandlingPathParam = BehandlingPathParam("referanse")
                )
            ) { params, _ ->
                val behandlingsreferanse = BehandlingReferanse(params.referanse)
                val avklaringsbehovKode = AvklaringsbehovKode.valueOf(params.avklaringsbehovkode)
                dataSource.transaction { connection ->
                    val repositoryProvider = repositoryRegistry.provider(connection)
                    val mellomlagretVurderingRepository = repositoryProvider.provide<MellomlagretVurderingRepository>()
                    val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                    val behandling = behandlingRepository.hent(behandlingsreferanse)
                    LoggingKontekst(
                        repositoryProvider,
                        LogKontekst(referanse = behandlingsreferanse)
                    ).use {
                        mellomlagretVurderingRepository.slett(
                            behandling.id,
                            avklaringsbehovKode
                        )
                    }
                }
                respondWithStatus(HttpStatusCode.Accepted)
            }
        }
    }
}

private fun MellomlagretVurdering.tilResponse() = MellomlagretVurderingDto(
    behandlingId = this.behandlingId,
    avklaringsbehovkode = this.avklaringsbehovKode,
    data = this.data,
    vurdertAv = this.vurdertAv,
    vurdertDato = this.vurdertDato,
)
