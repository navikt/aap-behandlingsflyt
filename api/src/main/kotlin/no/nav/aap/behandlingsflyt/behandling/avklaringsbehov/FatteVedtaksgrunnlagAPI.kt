package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import com.papsign.ktor.openapigen.route.tag
import no.nav.aap.behandlingsflyt.Tags
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.flate.Aksjon
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.flate.DefinisjonEndring
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.flate.Historikk
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.vedtak.TotrinnsVurdering
import no.nav.aap.behandlingsflyt.flyt.BehandlingFlyt
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Status
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.behandlingsflyt.tilgang.TilgangGateway
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.httpklient.auth.Bruker
import no.nav.aap.komponenter.httpklient.auth.bruker
import no.nav.aap.komponenter.httpklient.auth.token
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.komponenter.verdityper.Interval
import no.nav.aap.tilgang.AuthorizationParamPathConfig
import no.nav.aap.tilgang.BehandlingPathParam
import no.nav.aap.tilgang.authorizedGet
import java.time.LocalDateTime
import java.util.UUID
import javax.sql.DataSource

fun NormalOpenAPIRoute.fatteVedtakGrunnlagApi(dataSource: DataSource, repositoryRegistry: RepositoryRegistry) {
    route("/api/behandling").tag(Tags.Behandling) {
        route("/{referanse}/grunnlag/fatte-vedtak") {
            authorizedGet<BehandlingReferanse, FatteVedtakGrunnlagDto>(
                AuthorizationParamPathConfig(
                    behandlingPathParam = BehandlingPathParam(
                        ("referanse")
                    )
                )
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
                    val flyt = behandling.flyt()

                    val vurderinger = beslutterVurdering(avklaringsbehovene, flyt)

                    FatteVedtakGrunnlagDto(
                        harTilgangTilÅSaksbehandle = utledHarTilgangTilÅSaksbehandle(
                            req.referanse,
                            token(),
                            avklaringsbehovene,
                            bruker()
                        ),
                        vurderinger = vurderinger,
                        historikk = utledHistorikk(avklaringsbehovene)
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
    val harTilgang = GatewayProvider.provide<TilgangGateway>().sjekkTilgangTilBehandling(
        behandlingReferanse,
        Definisjon.FATTE_VEDTAK,
        token
    )

    val harIkkeGjortNoenVurderinger =
        avklaringsbehovene.alle().filter { it.erTotrinn() }.any { !it.brukere().contains(bruker.ident) }

    return harTilgang && harIkkeGjortNoenVurderinger
}


fun utledHistorikk(avklaringsbehovene: Avklaringsbehovene): List<Historikk> {
    val relevanteBehov =
        avklaringsbehovene.hentBehovForDefinisjon(listOf(Definisjon.FORESLÅ_VEDTAK, Definisjon.FATTE_VEDTAK))
    val alleBehov = avklaringsbehovene.alle()
        .filterNot { behov -> behov.definisjon in listOf(Definisjon.FORESLÅ_VEDTAK, Definisjon.FATTE_VEDTAK) }
    var tidsstempelForrigeBehov = LocalDateTime.MIN

    return relevanteBehov
        .asSequence()
        .flatMap { behov ->
            behov.historikk.filter { e -> e.status in listOf(Status.AVSLUTTET) }
                .map { endring -> DefinisjonEndring(behov.definisjon, endring) }
        }
        .sorted()
        .map { behov ->
            val aksjon = if (behov.definisjon == Definisjon.FORESLÅ_VEDTAK) {
                Aksjon.SENDT_TIL_BESLUTTER
            } else {
                val endringerSidenSist =
                    utledEndringerSidenSist(alleBehov, tidsstempelForrigeBehov, behov.endring.tidsstempel)
                if (endringerSidenSist.any { it.endring.status == Status.SENDT_TILBAKE_FRA_BESLUTTER }) {
                    Aksjon.RETURNERT_FRA_BESLUTTER
                } else {
                    Aksjon.FATTET_VEDTAK
                }
            }
            tidsstempelForrigeBehov = behov.endring.tidsstempel
            Historikk(aksjon, behov.endring.tidsstempel, behov.endring.endretAv)
        }.sorted()
        .toList()
}

private fun utledEndringerSidenSist(
    alleBehov: List<Avklaringsbehov>,
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

private fun beslutterVurdering(avklaringsbehovene: Avklaringsbehovene, flyt: BehandlingFlyt): List<TotrinnsVurdering> {
    return avklaringsbehovene.alle()
        .filter { it.erTotrinn() }
        .sortedWith { o1, o2 -> flyt.compareable().compare(o1.løsesISteg(), o2.løsesISteg()) }
        .map { tilKvalitetssikring(it) }
}

private fun tilKvalitetssikring(it: Avklaringsbehov): TotrinnsVurdering {
    return if (it.erTotrinnsVurdert() || it.harVærtSendtTilbakeFraBeslutterTidligere()) {
        val sisteVurdering =
            it.historikk.lastOrNull { it.status in setOf(Status.SENDT_TILBAKE_FRA_BESLUTTER, Status.TOTRINNS_VURDERT) }
        val godkjent = it.status() == Status.TOTRINNS_VURDERT

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
