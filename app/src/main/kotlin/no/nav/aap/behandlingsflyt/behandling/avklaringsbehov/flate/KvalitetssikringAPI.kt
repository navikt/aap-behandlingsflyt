package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.flate

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import com.zaxxer.hikari.HikariDataSource
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepositoryImpl
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.vedtak.TotrinnsVurdering
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Status
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.verdityper.Interval
import java.time.LocalDateTime

fun NormalOpenAPIRoute.kvalitetssikringApi(dataSource: HikariDataSource) {
    route("/api/behandling") {
        route("/{referanse}/grunnlag/kvalitetssikring") {
            get<BehandlingReferanse, KvalitetssikringGrunnlagDto> { req ->

                val dto = dataSource.transaction { connection ->
                    val behandling: Behandling =
                        BehandlingReferanseService(BehandlingRepositoryImpl(connection)).behandling(req)
                    val avklaringsbehovene =
                        AvklaringsbehovRepositoryImpl(connection).hentAvklaringsbehovene(behandling.id)

                    val vurderinger = kvalitetssikringsVurdering(avklaringsbehovene)
                    KvalitetssikringGrunnlagDto(
                        vurderinger = vurderinger,
                        historikk = utledKvalitetssikringHistorikk(avklaringsbehovene)
                    )
                }
                respond(dto)
            }
        }
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
        .map { behov ->
            behov.historikk.filter { e -> e.status in listOf(Status.OPPRETTET, Status.AVSLUTTET) }
                .map { endring -> DefinisjonEndring(behov.definisjon, endring) }
        }
        .flatten()
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
