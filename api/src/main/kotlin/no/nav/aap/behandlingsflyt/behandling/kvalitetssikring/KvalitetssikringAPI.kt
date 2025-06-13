package no.nav.aap.behandlingsflyt.behandling.kvalitetssikring

import com.papsign.ktor.openapigen.route.TagModule
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.Tags
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.KanIkkeVurdereEgneVurderingerException
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.flate.Aksjon
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.flate.DefinisjonEndring
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.flate.Historikk
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.vedtak.TotrinnsVurdering
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Status
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.behandlingsflyt.tilgang.TilgangGateway
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.httpklient.auth.Bruker
import no.nav.aap.komponenter.httpklient.auth.bruker
import no.nav.aap.komponenter.httpklient.auth.token
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken
import no.nav.aap.komponenter.miljo.Miljø
import no.nav.aap.komponenter.miljo.MiljøKode
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.komponenter.verdityper.Interval
import no.nav.aap.tilgang.AuthorizationParamPathConfig
import no.nav.aap.tilgang.BehandlingPathParam
import no.nav.aap.tilgang.authorizedGet
import java.time.LocalDateTime
import java.util.UUID
import javax.sql.DataSource
import kotlin.collections.any

fun NormalOpenAPIRoute.kvalitetssikringApi(dataSource: DataSource, repositoryRegistry: RepositoryRegistry) {
    route("/api/behandling") {
        route("/{referanse}/grunnlag/kvalitetssikring") {
            authorizedGet<BehandlingReferanse, KvalitetssikringGrunnlagDto>(
                AuthorizationParamPathConfig(behandlingPathParam = BehandlingPathParam("referanse")),
                auditLogConfig = null,
                TagModule(listOf(Tags.Grunnlag))
            ) { req ->

                val dto = dataSource.transaction(readOnly = true) { connection ->
                    val repositoryProvider = repositoryRegistry.provider(connection)
                    val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                    val avklaringsbehovRepository =
                        repositoryProvider.provide<AvklaringsbehovRepository>()

                    val behandling: Behandling =
                        BehandlingReferanseService(behandlingRepository).behandling(req)
                    val avklaringsbehovene =
                        avklaringsbehovRepository.hentAvklaringsbehovene(behandling.id)

                    val vurderinger = kvalitetssikringsVurdering(avklaringsbehovene)

                    KvalitetssikringGrunnlagDto(
                        harTilgangTilÅSaksbehandle = utledHarTilgangTilÅSaksbehandle(
                            req.referanse,
                            token(),
                            avklaringsbehovene,
                            bruker()
                        ),
                        vurderinger = vurderinger,
                        historikk = utledKvalitetssikringHistorikk(avklaringsbehovene)
                    )
                }
                respond(dto)
            }
        }
    }
}

private fun utledHarTilgangTilÅSaksbehandle(
    behandlingReferanse: UUID,
    token: OidcToken,
    avklaringsbehovene: Avklaringsbehovene,
    bruker: Bruker
): Boolean {
    val unleashGateway = GatewayProvider.provide<UnleashGateway>()
    val harTilgang = GatewayProvider.provide<TilgangGateway>().sjekkTilgangTilBehandling(
        behandlingReferanse,
        Definisjon.KVALITETSSIKRING,
        token
    )

    if (!unleashGateway.isEnabled(BehandlingsflytFeature.IngenValidering, bruker.ident)) {
        val harIkkeGjortNoenVurderinger =
            avklaringsbehovene.alle().filter { it.kreverKvalitetssikring() }
                .any { !it.brukere().contains(bruker.ident) }

        return harTilgang && harIkkeGjortNoenVurderinger
    } else {
        return harTilgang
    }

}

private fun utledKvalitetssikringHistorikk(avklaringsbehovene: Avklaringsbehovene): List<Historikk> {
    val relevanteBehov =
        avklaringsbehovene.hentBehovForDefinisjon(listOf(Definisjon.KVALITETSSIKRING))
    val alleBehov = avklaringsbehovene.alle()
        .filter { behov -> behov.definisjon in Definisjon.entries.filter { it.kvalitetssikres } }
    var tidsstempelForrigeBehov = LocalDateTime.MIN

    return relevanteBehov
        .asSequence()
        .flatMap { behov ->
            behov.historikk.filter { e -> e.status in listOf(Status.OPPRETTET, Status.AVSLUTTET) }
                .map { endring -> DefinisjonEndring(behov.definisjon, endring) }
        }
        .sorted()
        .map { behov ->
            val aksjon = if (behov.endring.status == Status.OPPRETTET) {
                Aksjon.SENDT_TIL_KVALITETSSIKRER
            } else {
                val endringerSidenSist =
                    utledEndringerSidenSist(alleBehov, tidsstempelForrigeBehov, behov.endring.tidsstempel)
                tidsstempelForrigeBehov = behov.endring.tidsstempel
                if (endringerSidenSist.any { it.endring.status == Status.SENDT_TILBAKE_FRA_KVALITETSSIKRER }) {
                    Aksjon.RETURNERT_FRA_KVALITETSSIKRER
                } else {
                    Aksjon.KVALITETSSIKRET
                }
            }

            Historikk(aksjon, behov.endring.tidsstempel, behov.endring.endretAv)
        }.sorted()
        .toList()
}

private fun utledEndringerSidenSist(
    alleBehov: List<no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehov>,
    tidsstempelForrigeBehov: LocalDateTime,
    tidsstempel: LocalDateTime
): List<DefinisjonEndring> {
    return alleBehov.map { behov ->
        behov.historikk.filter {
            Interval(
                tidsstempelForrigeBehov,
                tidsstempel
            ).inneholder(it.tidsstempel)
        }.map { endring -> DefinisjonEndring(behov.definisjon, endring) }
    }.flatten()
}

private fun kvalitetssikringsVurdering(avklaringsbehovene: Avklaringsbehovene): List<TotrinnsVurdering> {
    return avklaringsbehovene.alle()
        .filter { it.definisjon.kvalitetssikres }
        .map { tilKvalitetssikring(it) }
}

private fun tilKvalitetssikring(it: no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehov): TotrinnsVurdering {
    return if (it.erKvalitetssikretTidligere() || it.harVærtSendtTilbakeFraKvalitetssikrerTidligere()) {
        val sisteVurdering =
            it.historikk.lastOrNull {
                it.status in setOf(
                    Status.SENDT_TILBAKE_FRA_KVALITETSSIKRER,
                    Status.KVALITETSSIKRET
                )
            }
        val godkjent = it.status() == Status.KVALITETSSIKRET

        TotrinnsVurdering(
            it.definisjon.kode,
            godkjent,
            sisteVurdering?.begrunnelse,
            sisteVurdering?.årsakTilRetur ?: emptyList()
        )
    } else {
        TotrinnsVurdering(it.definisjon.kode, null, null, listOf())
    }
}
