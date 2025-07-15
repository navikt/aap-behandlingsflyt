package no.nav.aap.behandlingsflyt.behandling.lovvalgmedlemskap.grunnlag

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.behandling.ansattinfo.AnsattInfoService
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.MedlemskapArbeidInntektForutgåendeRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.behandlingsflyt.tilgang.kanSaksbehandle
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.tilgang.BehandlingPathParam
import no.nav.aap.tilgang.getGrunnlag
import javax.sql.DataSource

fun NormalOpenAPIRoute.forutgåendeMedlemskapAPI(dataSource: DataSource, repositoryRegistry: RepositoryRegistry) {
    route("/api/behandling") {
        route("/{referanse}/grunnlag/forutgaaendemedlemskap") {
            getGrunnlag<BehandlingReferanse, ForutgåendeMedlemskapGrunnlagResponse>(
                behandlingPathParam = BehandlingPathParam("referanse"),
                avklaringsbehovKode =  Definisjon.AVKLAR_FORUTGÅENDE_MEDLEMSKAP.kode.toString()
            ) { req ->
                val grunnlag = dataSource.transaction { connection ->
                    val repositoryProvider = repositoryRegistry.provider(connection)
                    val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                    val forutgåendeRepository =
                        repositoryProvider.provide<MedlemskapArbeidInntektForutgåendeRepository>()
                    val behandling = BehandlingReferanseService(behandlingRepository).behandling(req)

                    val data = forutgåendeRepository.hentHvisEksisterer(behandling.id)?.manuellVurdering
                    val historiskeManuelleVurderinger =
                        forutgåendeRepository.hentHistoriskeVurderinger(behandling.sakId, behandling.id)
                    val ansattNavnOgEnhet = data?.let { AnsattInfoService().hentAnsattNavnOgEnhet(it.vurdertAv) }
                    

                    ForutgåendeMedlemskapGrunnlagResponse(
                        harTilgangTilÅSaksbehandle = kanSaksbehandle(),
                        vurdering = data?.toResponse(ansattNavnOgEnhet = ansattNavnOgEnhet),
                        historiskeManuelleVurderinger = historiskeManuelleVurderinger.map { it.toResponse() }
                    )
                }
                respond(grunnlag)
            }
        }
    }
}
