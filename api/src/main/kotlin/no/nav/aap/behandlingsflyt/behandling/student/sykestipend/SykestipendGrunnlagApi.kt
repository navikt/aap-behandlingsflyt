package no.nav.aap.behandlingsflyt.behandling.student.sykestipend

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvService
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.AndreUtbetalingerYtelser
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.andreYtelserOppgittISøknad.AndreYtelserOppgittISøknadRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.sykestipend.SykestipendRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.behandlingsflyt.tilgang.kanSaksbehandle
import no.nav.aap.behandlingsflyt.tilgang.relevanteIdenterForBehandlingResolver
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.tilgang.BehandlingPathParam
import no.nav.aap.tilgang.getGrunnlag
import javax.sql.DataSource

fun NormalOpenAPIRoute.sykestipendGrunnlagApi(
    dataSource: DataSource,
    repositoryRegistry: RepositoryRegistry,
    gatewayProvider: GatewayProvider
) {
    route("/api/behandling") {
        route("/{referanse}/grunnlag/sykestipend") {
            getGrunnlag<BehandlingReferanse, SykestipendGrunnlagResponse>(
                relevanteIdenterResolver = relevanteIdenterForBehandlingResolver(repositoryRegistry, dataSource),
                behandlingPathParam = BehandlingPathParam("referanse"),
                avklaringsbehovKode = Definisjon.AVKLAR_SAMORDNING_SYKESTIPEND.kode.toString()
            ) { req ->
                val response = dataSource.transaction(readOnly = true) { connection ->
                    val repositoryProvider = repositoryRegistry.provider(connection)
                    val sykestipendRepository = repositoryProvider.provide<SykestipendRepository>()
                    val vurdertAvService = VurdertAvService(repositoryProvider, gatewayProvider)


                    val behandling =
                        BehandlingReferanseService(repositoryProvider.provide())
                            .behandling(req)

                    val gjeldendeVurdering = sykestipendRepository.hentHvisEksisterer(behandlingId = behandling.id)

                    val andreYtelserRepository = repositoryProvider.provide<AndreYtelserOppgittISøknadRepository>()
                    val andreUtbetalinger = andreYtelserRepository.hentHvisEksisterer(behandling.id)

                    val svarFraSøknad = andreUtbetalinger?.stønad?.contains(AndreUtbetalingerYtelser.STIPEND_FRA_LÅNEKASSEN)


                    SykestipendGrunnlagResponse(
                        harTilgangTilÅSaksbehandle = kanSaksbehandle(),
                        historiskeVurderinger = emptyList(),
                        gjeldendeVurdering = gjeldendeVurdering?.let {
                            SykestipendvurderingResponse.fraDomene(
                                it.vurdering,
                                vurdertAvService
                            )
                        },
                        sykeStipendSvarFraSøknad = svarFraSøknad
                    )

                }
                respond(response)
            }
        }
    }
}
