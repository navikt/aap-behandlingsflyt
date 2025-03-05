package no.nav.aap.behandlingsflyt.behandling.lovvalgmedlemskap.grunnlag

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.MedlemskapArbeidInntektForutgåendeRepository
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.lookup.repository.RepositoryProvider
import javax.sql.DataSource

fun NormalOpenAPIRoute.forutgåendeMedlemskapAPI(dataSource: DataSource) {
    route("/api/behandling") {
        route("/{referanse}/grunnlag/forutgaaendemedlemskap") {
            get<BehandlingReferanse, ForutgåendeMedlemskapGrunnlagDto> { req ->
                val grunnlag = dataSource.transaction { connection ->
                    val repositoryProvider = RepositoryProvider(connection)
                    val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                    val forutgåendeRepository = repositoryProvider.provide<MedlemskapArbeidInntektForutgåendeRepository>()
                    val behandling = BehandlingReferanseService(behandlingRepository).behandling(req)

                    val data = forutgåendeRepository.hentHvisEksisterer(behandling.id)?.manuellVurdering
                    val historiskeManuelleVurderinger = forutgåendeRepository.hentHistoriskeVurderinger(behandling.sakId)

                    ForutgåendeMedlemskapGrunnlagDto(data, historiskeManuelleVurderinger)
                }
                respond(grunnlag)
            }
        }
    }
}