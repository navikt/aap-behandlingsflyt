package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.flate

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import com.zaxxer.hikari.HikariDataSource
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepositoryImpl
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Status
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.vedtak.TotrinnsVurdering
import no.nav.aap.behandlingsflyt.dbconnect.transaction
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.verdityper.Interval
import java.time.LocalDateTime

fun NormalOpenAPIRoute.fatteVedtakGrunnlagApi(dataSource: HikariDataSource) {
    route("/api/behandling") {
        route("/{referanse}/grunnlag/fatte-vedtak") {
            get<BehandlingReferanse, FatteVedtakGrunnlagDto> { req ->

                val dto = dataSource.transaction { connection ->
                    val behandling: Behandling =
                        BehandlingReferanseService(BehandlingRepositoryImpl(connection)).behandling(req)
                    val avklaringsbehovene =
                        AvklaringsbehovRepositoryImpl(connection).hentAvklaringsbehovene(behandling.id)

                    val vurderinger = kvalitetssikringsVurdering(avklaringsbehovene)
                    FatteVedtakGrunnlagDto(vurderinger = vurderinger, historikk = utledHistorikk(avklaringsbehovene))
                }
                respond(dto)
            }
        }
    }
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
        .filter { it.erTotrinn() }
        .map { tilKvalitetssikring(it) }
}

private fun tilKvalitetssikring(it: no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehov): TotrinnsVurdering {
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
