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
import no.nav.aap.behandlingsflyt.tilgang.kanSaksbehandle
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
            authorizedPost<Unit, MellomlagretVurderingDto, MellomlagretVurderingRequest>(
                AuthorizationBodyPathConfig(
                    operasjon = Operasjon.SAKSBEHANDLE,
                )
            ) { _, request ->
                dataSource.transaction { connection ->
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

                        mellomlagretVurderingRepository.lagre(
                            MellomlagretVurdering(
                                behandlingId = behandling.id,
                                avklaringsbehovKode = avklaringsbehovKode,
                                data = request.data,
                                vurdertAv = bruker().ident,
                                vurdertDato = LocalDateTime.now()
                            )
                        )
                    }
                }
                respondWithStatus(HttpStatusCode.Accepted)
            }
        }
        route("/mellomlagret-vurdering/{referanse}/{avklaringsbehovkode}") {
            authorizedGet<BehandlingReferanseMedAvklaringsbehov, MellomlagredeVurderingResponse>(
                AuthorizationParamPathConfig(
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
                        MellomlagredeVurderingResponse(
                            mellomlagretVurdering = mellomlagretVurdering?.tilResponse(),
                            harTilgangTilÅSaksbehandle = kanSaksbehandle(),
                        )
                    }
                }
                respond(response)
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
