package no.nav.aap.behandlingsflyt.behandling.grunnlag.avslag_11_27

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.behandling.avslag11_27.Avslag11_27Repository
import no.nav.aap.behandlingsflyt.behandling.avslag11_27.Avslag11_27Vurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.KravRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.NyttKrav
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.tilgang.kanSaksbehandle
import no.nav.aap.behandlingsflyt.tilgang.relevanteIdenterForBehandlingResolver
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.tilgang.AuthorizationParamPathConfig
import no.nav.aap.tilgang.BehandlingPathParam
import no.nav.aap.tilgang.Operasjon
import no.nav.aap.tilgang.authorizedGet
import javax.sql.DataSource

fun NormalOpenAPIRoute.avslag11_27GrunnlagApi(
    dataSource: DataSource,
    repositoryRegistry: RepositoryRegistry,
    gatewayProvider: GatewayProvider,
) {
    route("/api/behandling/{referanse}/grunnlag/avslag-11-27").authorizedGet<BehandlingReferanse, Avslag11_27GrunnlagDto>(
        AuthorizationParamPathConfig(
            relevanteIdenterResolver = relevanteIdenterForBehandlingResolver(repositoryRegistry, dataSource),
            operasjon = Operasjon.SE,
            behandlingPathParam = BehandlingPathParam("referanse")
        )
    ) { req ->
        val avslag11_27grunnlagDto = dataSource.transaction(readOnly = true) { connection ->
            val repositoryProvider = repositoryRegistry.provider(connection)
            val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
            val avslag_11_27Repository = repositoryProvider.provide<Avslag11_27Repository>()
            val kravRepository = repositoryProvider.provide<KravRepository>()

            val behandling = behandlingRepository.hent(BehandlingReferanse(req.referanse))
            val nyttKravListe =
                kravRepository.hentHvisEksisterer(behandling.id)?.vurderinger?.filterIsInstance<NyttKrav>()
                    ?: emptyList()

            val nyVurderinger =
                behandling.id.let { avslag_11_27Repository.hentHvisEksisterer(it) }?.vurderinger.orEmpty()

            val vedtatteVurderinger =
                behandling.forrigeBehandlingId?.let { avslag_11_27Repository.hentHvisEksisterer(it) }?.vurderinger.orEmpty()

            val nyttKravListeDto = Avslag11_27KravDto.avslag11_27TilDto(nyttKravListe);

            Avslag11_27GrunnlagDto(
                harTilgangTilÅSaksbehandle = kanSaksbehandle(),
                krav = nyttKravListeDto,
                vurderinger = mapVurderingerTilDto(nyVurderinger),
                vedtatteVurdering = mapVurderingerTilDto(vedtatteVurderinger)
            )
        }

        respond(avslag11_27grunnlagDto)
    }
}

private fun mapVurderingerTilDto(vurderinger: List<Avslag11_27Vurdering>): List<Avslag11_27VurderingDto> {
    return vurderinger.map { vurdering ->
        Avslag11_27VurderingDto(
            journalpostId = vurdering.journalpostId.identifikator,
            begrunnelse = vurdering.begrunnelse,
            harAnnenFullYtelse = vurdering.harAnnenFullYtelse,
            brukersYtelse = vurdering.brukersYtelse,
            harSykepengegrunnlagOver2G = vurdering.harSykepengegrunnlagOver2G,
            skalAvslås1127 = vurdering.skalAvslås1127
        )
    }
}
