package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsevne.flate

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.response.respondWithStatus
import com.papsign.ktor.openapigen.route.route
import com.zaxxer.hikari.HikariDataSource
import io.ktor.http.*
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsevne.ArbeidsevnePerioder
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsevne.ArbeidsevneRepository
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.komponenter.dbconnect.transaction

fun NormalOpenAPIRoute.arbeidsevneGrunnlagApi(dataSource: HikariDataSource) {
    route("/api/behandling/{referanse}/grunnlag/arbeidsevne") {
        get<BehandlingReferanse, ArbeidsevneGrunnlagDto> { behandlingReferanse ->
            arbeidsevneGrunnlag(dataSource, behandlingReferanse)?.let { respond(it) } ?: respondWithStatus(
                HttpStatusCode.NoContent
            )
        }

        route("/simulering") {
            post<BehandlingReferanse, SimulertArbeidsevneResultatDto, SimulerArbeidsevneDto> { behandlingReferanse, dto ->
                respond(simuleringsresulat(dataSource, behandlingReferanse, dto))
            }
        }

    }
}

private fun arbeidsevneGrunnlag(dataSource: HikariDataSource, behandlingReferanse: BehandlingReferanse): ArbeidsevneGrunnlagDto? {
    return dataSource.transaction { connection ->
        val behandling: Behandling =
            BehandlingReferanseService(BehandlingRepositoryImpl(connection)).behandling(behandlingReferanse)
        val arbeidsevneRepository = ArbeidsevneRepository(connection)

        val nåTilstand = arbeidsevneRepository.hentHvisEksisterer(behandling.id)?.vurderinger ?: return@transaction null
        val vedtatteVerdier =
            behandling.forrigeBehandlingId?.let { arbeidsevneRepository.hentHvisEksisterer(it) }?.vurderinger.orEmpty()
        val historikk = arbeidsevneRepository.hentAlleVurderinger(behandling.sakId, behandling.id)

        ArbeidsevneGrunnlagDto(
            historikk = historikk.map { it.toDto() }.sortedBy { it.vurderingsTidspunkt }.toSet(),
            vurderinger = nåTilstand.filterNot { vedtatteVerdier.contains(it) }.map { it.toDto() }
                .sortedBy { it.fraDato },
            gjeldendeVedtatteVurderinger = vedtatteVerdier.map { it.toDto() }.sortedBy { it.fraDato }
        )
    }
}

private fun simuleringsresulat(
    dataSource: HikariDataSource,
    behandlingReferanse: BehandlingReferanse,
    dto: SimulerArbeidsevneDto
): SimulertArbeidsevneResultatDto {
    return dataSource.transaction { con ->
        val arbeidsevneRepository = ArbeidsevneRepository(con)
        val behandling = BehandlingReferanseService(BehandlingRepositoryImpl(con)).behandling(behandlingReferanse)

        val vedtatteArbeidsevner =
            behandling.forrigeBehandlingId?.let { arbeidsevneRepository.hentHvisEksisterer(it) }?.vurderinger.orEmpty()
        val nåværendeArbeidsevnePerioder = ArbeidsevnePerioder(vedtatteArbeidsevner)
        val simuleringsresultat = nåværendeArbeidsevnePerioder.leggTil(
            ArbeidsevnePerioder(dto.vurderinger.map { it.toArbeidsevnevurdering() })
        )

        SimulertArbeidsevneResultatDto(simuleringsresultat.gjeldendeArbeidsevner().map { it.toDto() })
    }
}