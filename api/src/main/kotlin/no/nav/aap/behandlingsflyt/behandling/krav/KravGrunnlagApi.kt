package no.nav.aap.behandlingsflyt.behandling.krav

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.KravRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.behandlingsflyt.tilgang.kanSaksbehandle
import no.nav.aap.behandlingsflyt.tilgang.relevanteIdenterForBehandlingResolver
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.tilgang.BehandlingPathParam
import no.nav.aap.tilgang.getGrunnlag
import javax.sql.DataSource
import kotlin.collections.orEmpty
import kotlin.collections.sortedBy

fun NormalOpenAPIRoute.kravGrunnlagApi(
    dataSource: DataSource,
    repositoryRegistry: RepositoryRegistry
) {
    route("api/behandling/{referanse}/grunnlag/krav") {
        getGrunnlag<BehandlingReferanse, KravGrunnlagDto>(
            relevanteIdenterResolver = relevanteIdenterForBehandlingResolver(repositoryRegistry, dataSource),
            behandlingPathParam = BehandlingPathParam("referanse"),
            påkrevdRolle = Definisjon.VURDER_KRAV.løsesAv
        ) { req ->
            val response = dataSource.transaction(readOnly = true) { connection ->
                val repositoryProvider = repositoryRegistry.provider(connection)
                val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                val kravRepository: KravRepository = repositoryProvider.provide()

                val behandling = BehandlingReferanseService(behandlingRepository).behandling(req)


                val kravGrunnlag = kravRepository.hentHvisEksisterer(behandlingId = behandling.id)


                val nyeVurderinger = kravGrunnlag
                    ?.vurderinger?.filter { it.vurdertIBehandling == behandling.id }.orEmpty()
                    .sortedBy { it.journalpostId.identifikator }
                    .map { it.somDto() }

                val sisteVedtatte =
                    kravGrunnlag?.gjeldendeVedtatteVurderinger(behandling.id).orEmpty().map { it.somDto() }

                KravGrunnlagDto(
                    harTilgangTilÅSaksbehandle = kanSaksbehandle(),
                    nyeVurderinger = nyeVurderinger,
                    vedtatteVurderinger = sisteVedtatte
                )

            }
            respond(response)

        }
    }
}