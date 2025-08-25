package no.nav.aap.behandlingsflyt.hendelse

import com.papsign.ktor.openapigen.route.TagModule
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.*
import no.nav.aap.behandlingsflyt.Azp
import no.nav.aap.behandlingsflyt.EMPTY_JSON_RESPONSE
import no.nav.aap.behandlingsflyt.Tags
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Innsending
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.ManuellRevurdering
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.ManuellRevurderingV0
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.flate.SaksnummerParameter
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.komponenter.server.auth.bruker
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.tilgang.AuthorizationMachineToMachineConfig
import no.nav.aap.tilgang.AuthorizationParamPathConfig
import no.nav.aap.tilgang.SakPathParam
import no.nav.aap.tilgang.authorizedPost
import org.slf4j.MDC
import javax.sql.DataSource

fun NormalOpenAPIRoute.mottattHendelseApi(
    dataSource: DataSource,
    repositoryRegistry: RepositoryRegistry,
    gatewayProvider: GatewayProvider,
) {
    val unleashGateway = gatewayProvider.provide<UnleashGateway>()
    route("/api/hendelse") {
        route("/send") {
            authorizedPost<Unit, String, Innsending>(
                modules = arrayOf(TagModule(listOf(Tags.Sak))),
                routeConfig = AuthorizationMachineToMachineConfig(
                    authorizedAzps = mutableListOf(
                        Azp.Postmottak.uuid,
                        Azp.Dokumentinnhenting.uuid
                    )
                )
            ) { _, dto ->
                MDC.putCloseable("saksnummer", dto.saksnummer.toString()).use {
                    dataSource.transaction { connection ->
                        val repositoryRegistry = repositoryRegistry.provider(connection)
                        MottattHendelseService(repositoryRegistry).registrerMottattHendelse(dto)
                    }
                }
                respond(EMPTY_JSON_RESPONSE, HttpStatusCode.Accepted)
            }
        }

        route("/sak/{saksnummer}/send") {
            // TODO: Fikse bug i tilgang for å sikre endepunktet
            authorizedPost<SaksnummerParameter, String, Innsending>(
                modules = arrayOf(TagModule(listOf(Tags.Sak))),
                routeConfig = AuthorizationParamPathConfig(
                    sakPathParam = SakPathParam("saksnummer"),
                )
            ) { _, dto ->
                validerHendelse(dto, bruker(), unleashGateway)
                MDC.putCloseable("saksnummer", dto.saksnummer.toString()).use {
                    dataSource.transaction { connection ->
                        val repositoryRegistry = repositoryRegistry.provider(connection)
                        MottattHendelseService(repositoryRegistry).registrerMottattHendelse(dto)
                    }
                }
                respond(EMPTY_JSON_RESPONSE, HttpStatusCode.Accepted)
            }
        }
    }
}

fun validerHendelse(
    innsending: Innsending,
    bruker: Bruker,
    unleashGateway: UnleashGateway,
) {
    if (innsending.type == InnsendingType.MANUELL_REVURDERING && innsending.melding is ManuellRevurdering) {
        val melding = innsending.melding as ManuellRevurderingV0
        val gjelderOverstyringAvStarttidspunkt =
            melding.årsakerTilBehandling.contains(Vurderingsbehov.VURDER_RETTIGHETSPERIODE)
        if (gjelderOverstyringAvStarttidspunkt && !unleashGateway.isEnabled(
                BehandlingsflytFeature.OverstyrStarttidspunkt,
                bruker.ident
            )
        ) {
            throw UgyldigForespørselException("Funksjonsbryter for overstyr starttidspunkt er skrudd av")
        }
    }
}