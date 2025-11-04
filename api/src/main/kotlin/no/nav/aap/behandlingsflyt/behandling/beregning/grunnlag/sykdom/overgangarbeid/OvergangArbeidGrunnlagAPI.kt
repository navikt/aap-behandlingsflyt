package no.nav.aap.behandlingsflyt.behandling.beregning.grunnlag.sykdom.overgangarbeid

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.behandling.ansattinfo.AnsattInfoService
import no.nav.aap.behandlingsflyt.behandling.beregning.grunnlag.sykdom.utils.tilResponse
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvService
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangarbeid.OvergangArbeidGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangarbeid.OvergangArbeidRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.tilgang.kanSaksbehandle
import no.nav.aap.behandlingsflyt.tilgang.relevanteIdenterForBehandlingResolver
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.tilgang.BehandlingPathParam
import no.nav.aap.tilgang.getGrunnlag
import javax.sql.DataSource

fun NormalOpenAPIRoute.overgangArbeidGrunnlagApi(
    dataSource: DataSource, repositoryRegistry: RepositoryRegistry,
    gatewayProvider: GatewayProvider,
) {
    val ansattInfoService = AnsattInfoService(gatewayProvider)

    route("/api/behandling") {
        route("/{referanse}/grunnlag/overgangarbeid") {
            getGrunnlag<BehandlingReferanse, OvergangArbeidGrunnlagResponse>(
                relevanteIdenterResolver = relevanteIdenterForBehandlingResolver(repositoryRegistry, dataSource),
                behandlingPathParam = BehandlingPathParam("referanse"),
                avklaringsbehovKode = Definisjon.AVKLAR_OVERGANG_ARBEID.kode.toString()
            ) { req ->
                val respons = dataSource.transaction(readOnly = true) { connection ->
                    val repositoryProvider = repositoryRegistry.provider(connection)
                    val vurdertAvService = VurdertAvService(repositoryProvider, gatewayProvider)
                    val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                    val overgangArbeidRepository = repositoryProvider.provide<OvergangArbeidRepository>()
                    val sykdomRepository = repositoryProvider.provide<SykdomRepository>()
                    val sakRepository = repositoryProvider.provide<SakRepository>()
                    val behandlingReferanseService = BehandlingReferanseService(behandlingRepository)

                    val behandling = behandlingReferanseService.behandling(req)
                    val sak = sakRepository.hent(behandling.sakId)
                    val grunnlag = overgangArbeidRepository.hentHvisEksisterer(behandling.id)
                    val forrigeGrunnlag = behandling.forrigeBehandlingId
                        ?.let { overgangArbeidRepository.hentHvisEksisterer(it) }
                        ?: OvergangArbeidGrunnlag(emptyList())

                    OvergangArbeidGrunnlagResponse(
                        harTilgangTilÅSaksbehandle = kanSaksbehandle(),

                        sisteVedtatteVurderinger = OvergangArbeidVurderingResponse.fraDomene(
                            forrigeGrunnlag.gjeldendeVurderinger(),
                            vurdertAvService,
                        ),

                        nyeVurderinger = grunnlag?.vurderinger.orEmpty()
                            .filter { it.vurdertIBehandling == behandling.id }
                            .map { OvergangArbeidVurderingResponse.fraDomene(it, vurdertAvService) },

                        kanVurderes = listOf(sak.rettighetsperiode),

                        behøverVurderinger = listOf(sak.rettighetsperiode),

                        gjeldendeSykdsomsvurderinger = sykdomRepository.hentHvisEksisterer(behandling.id)
                            ?.sykdomsvurderinger.orEmpty()
                            .map { it.tilResponse(ansattInfoService) },
                    )
                }

                respond(respons)
            }
        }
    }
}
