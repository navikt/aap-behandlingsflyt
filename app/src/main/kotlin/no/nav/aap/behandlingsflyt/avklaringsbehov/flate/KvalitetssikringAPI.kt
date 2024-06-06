package no.nav.aap.behandlingsflyt.avklaringsbehov.flate

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import com.zaxxer.hikari.HikariDataSource
import no.nav.aap.behandlingsflyt.avklaringsbehov.Avklaringsbehov
import no.nav.aap.behandlingsflyt.avklaringsbehov.AvklaringsbehovRepositoryImpl
import no.nav.aap.behandlingsflyt.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.avklaringsbehov.Status
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.vedtak.TotrinnsVurdering
import no.nav.aap.behandlingsflyt.dbconnect.transaction
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.verdityper.Interval
import java.time.LocalDateTime

fun NormalOpenAPIRoute.kvalitetssikringApi(dataSource: HikariDataSource) {
    route("/api/behandling") {
        route("/{referanse}/grunnlag/kvalitetssikring") {
            get<BehandlingReferanse, KvalitetssikringGrunnlagDto> { req ->

                val dto = dataSource.transaction { connection ->
                    val behandling: Behandling = BehandlingReferanseService(connection).behandling(req)
                    val avklaringsbehovene =
                        AvklaringsbehovRepositoryImpl(connection).hentAvklaringsbehovene(behandling.id)

                    val vurderinger = kvalitetssikringsVurdering(avklaringsbehovene)
                    KvalitetssikringGrunnlagDto(vurderinger = vurderinger, historikk = utledKvalitetssikringHistorikk(avklaringsbehovene))
                }
                respond(dto)
            }
        }
    }
}

private fun utledKvalitetssikringHistorikk(avklaringsbehovene: Avklaringsbehovene): List<Historikk> {
    val relevanteBehov =
        avklaringsbehovene.hentBehovForDefinisjon(listOf(Definisjon.FORESLÅ_VEDTAK, Definisjon.FATTE_VEDTAK))
    val alleBehov = avklaringsbehovene.alle()
        .filterNot { behov -> behov.definisjon in listOf(Definisjon.FORESLÅ_VEDTAK, Definisjon.FATTE_VEDTAK) }
    var tidsstempelForrigeBehov = LocalDateTime.MIN

    return relevanteBehov
        .asSequence()
        .map { behov ->
            behov.historikk.filter { e -> e.status in listOf(Status.AVSLUTTET) }
                .map { endring -> DefinisjonEndring(behov.definisjon, endring) }
        }
        .flatten()
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

private fun kvalitetssikringsVurdering(avklaringsbehovene: Avklaringsbehovene): List<TotrinnsVurdering> {
    return avklaringsbehovene.alle()
        .filter { it.definisjon.kvalitetssikres }
        .map { tilKvalitetssikring(it) }
}

private fun tilKvalitetssikring(it: Avklaringsbehov): TotrinnsVurdering {
    return if (it.erKvalitetssikretTidligere() || it.harVærtSendtTilbakeFraKvalitetssikrerTidligere()) {
        val sisteVurdering =
            it.historikk.lastOrNull { it.status in setOf(Status.SENDT_TILBAKE_FRA_KVALITETSSIKRER, Status.KVALITETSSIKRET) }
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
