package no.nav.aap.behandlingsflyt.behandling.foreslåvedtak

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.behandling.foreslåvedtak.ForeslåVedtakDto.Companion.tilForeslåVedtakData
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.FORESLÅ_VEDTAK_KODE
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.tilgang.BehandlingPathParam
import no.nav.aap.tilgang.getGrunnlag
import javax.sql.DataSource

fun NormalOpenAPIRoute.foreslaaVedtakAPI(dataSource: DataSource, repositoryRegistry: RepositoryRegistry) {
    route("/api/behandling") {
        route("/{referanse}/grunnlag/foreslaa-vedtak").getGrunnlag<BehandlingReferanse, ForeslåVedtakResponse>(
            behandlingPathParam = BehandlingPathParam("referanse"),
            avklaringsbehovKode = FORESLÅ_VEDTAK_KODE
        ) { behandlingReferanse ->
            val underveisGrunnlag = dataSource.transaction(readOnly = true) { conn ->
                val repositoryProvider = repositoryRegistry.provider(conn)
                val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                val behandling =
                    BehandlingReferanseService(behandlingRepository).behandling(behandlingReferanse)
                val underveisRepository = repositoryProvider.provide<UnderveisRepository>()
                underveisRepository.hent(behandling.id)
            }

            val foreslåVedtakPerioder = underveisGrunnlag.perioder.map { ForeslåVedtakDto(
                it.periode,
                it.utfall,
                it.rettighetsType,
            ) }

            val perioderMedUtfallOgRettighetstype = foreslåVedtakPerioder
                .map { Segment(it.periode, it.tilForeslåVedtakData()) }
                .let(::Tidslinje).komprimer()
                .map { ForeslåVedtakDto(
                    periode = it.periode,
                    utfall = it.verdi.utfall,
                    rettighetsType = it.verdi.rettighetsType,
                    )
                }

            respond(ForeslåVedtakResponse(
                perioderMedUtfallOgRettighetstype
            ))
        }
    }
}