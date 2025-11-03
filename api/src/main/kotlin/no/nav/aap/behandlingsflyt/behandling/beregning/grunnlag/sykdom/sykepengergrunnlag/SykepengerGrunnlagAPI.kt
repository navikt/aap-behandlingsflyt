package no.nav.aap.behandlingsflyt.behandling.beregning.grunnlag.sykdom.sykepengergrunnlag

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.behandling.ansattinfo.AnsattInfoService
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvResponse
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykepengerErstatningRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykepengerVurdering
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.behandlingsflyt.tilgang.kanSaksbehandle
import no.nav.aap.behandlingsflyt.tilgang.relevanteIdenterForBehandlingResolver
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.tilgang.BehandlingPathParam
import no.nav.aap.tilgang.getGrunnlag
import javax.sql.DataSource


fun NormalOpenAPIRoute.sykepengerGrunnlagApi(
    dataSource: DataSource,
    repositoryRegistry: RepositoryRegistry,
    gatewayProvider: GatewayProvider,
) {
    val ansattInfoService = AnsattInfoService(gatewayProvider)

    route("/api/behandling") {
        route("/{referanse}/grunnlag/sykdom/sykepengergrunnlag") {
            getGrunnlag<BehandlingReferanse, SykepengerGrunnlagResponse>(
                relevanteIdenterResolver = relevanteIdenterForBehandlingResolver(repositoryRegistry, dataSource),
                behandlingPathParam = BehandlingPathParam("referanse"),
                avklaringsbehovKode = Definisjon.AVKLAR_SYKEPENGEERSTATNING.kode.toString()
            ) { req ->
                val response = dataSource.transaction(readOnly = true) { connection ->
                    val repositoryProvider = repositoryRegistry.provider(connection)
                    val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                    val sykepengerErstatningRepository = repositoryProvider.provide<SykepengerErstatningRepository>()
                    val behandling: Behandling = BehandlingReferanseService(behandlingRepository).behandling(req)

                    val vedtatteVurderinger =
                        behandling.forrigeBehandlingId?.let { sykepengerErstatningRepository.hentHvisEksisterer(it) }?.vurderinger.orEmpty()

                    val nåTilstand = sykepengerErstatningRepository.hentHvisEksisterer(behandling.id)?.vurderinger.orEmpty()

                    val vurdering = nåTilstand
                        .filterNot { gjeldendeVurdering -> gjeldendeVurdering.copy(vurdertTidspunkt = null) in vedtatteVurderinger.map { it.copy(vurdertTidspunkt = null) } }
                        .singleOrNull()

                    SykepengerGrunnlagResponse(
                        harTilgangTilÅSaksbehandle = kanSaksbehandle(),
                        vurdering = vurdering?.tilResponse(ansattInfoService),
                        vurderinger = nåTilstand.map { it.tilResponse(ansattInfoService) },
                        vedtatteVurderinger = vedtatteVurderinger.map { it.tilResponse(ansattInfoService) }
                    )
                }
                respond(response)
            }
        }
    }
}

private fun SykepengerVurdering.tilResponse(ansattInfoService: AnsattInfoService): SykepengerVurderingResponse {
    val navnOgEnhet = ansattInfoService.hentAnsattNavnOgEnhet(vurdertAv)
    return SykepengerVurderingResponse(
        begrunnelse = begrunnelse,
        dokumenterBruktIVurdering = dokumenterBruktIVurdering,
        harRettPå = harRettPå,
        grunn = grunn,
        gjelderFra = gjelderFra,
        vurdertAv = VurdertAvResponse(
            ident = vurdertAv,
            dato = vurdertTidspunkt?.toLocalDate() ?: error("Mangler dato for sykepengervurdering"),
            ansattnavn = navnOgEnhet?.navn,
            enhetsnavn = navnOgEnhet?.enhet
        )
    )
}
