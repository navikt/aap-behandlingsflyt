package no.nav.aap.behandlingsflyt.behandling.andrestatligeytelser

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.dagpenger.DagpengerRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.tilgang.relevanteIdenterForBehandlingResolver
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.tilgang.BehandlingPathParam
import no.nav.aap.tilgang.getGrunnlag
import javax.sql.DataSource

fun NormalOpenAPIRoute.andreStatligeYtelserGrunnlagApi(
    dataSource: DataSource,
    repositoryRegistry: RepositoryRegistry
) {
    route("/api/behandling/grunnlag/andrestatligeytelser/{referanse}") {
        getGrunnlag<BehandlingReferanse, AndreStatligeYtelserGrunnlagDto>(
            relevanteIdenterResolver = relevanteIdenterForBehandlingResolver(repositoryRegistry, dataSource),
            behandlingPathParam = BehandlingPathParam("referanse"),
            avklaringsbehovKode = Definisjon.SAMORDNING_ANDRE_STATLIGE_YTELSER.toString()
        )
        {behandlingReferanse ->
            val andreStatligeYtelserGrunnlag = dataSource.transaction { connection ->
                val repositoryProvider = repositoryRegistry.provider(connection)
                val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                val sakRepository = repositoryProvider.provide<SakRepository>()

                val behandling: Behandling =
                    BehandlingReferanseService(behandlingRepository).behandling(behandlingReferanse)

                val andreStatligeYtelserRepository = repositoryProvider.provide<DagpengerRepository>()

                val dagpengerGrunnlag = andreStatligeYtelserRepository.hent(behandling.id).map {
                    DagpengerPeriodeDto(
                        fom = it.periode.fom,
                        tom = it.periode.tom,
                        dagpengerYtelseType= it.dagpengerYtelseType,
                        kilde = it.kilde
                    )
                }

                AndreStatligeYtelserGrunnlagDto(dagpengerGrunnlag)
            }
            respond(andreStatligeYtelserGrunnlag)
        }
    }
}
