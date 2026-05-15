package no.nav.aap.behandlingsflyt.behandling.søknad

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurderingerMetaResponse
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvService
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.tilgang.relevanteIdenterForBehandlingResolver
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.tilgang.AuthorizationParamPathConfig
import no.nav.aap.tilgang.BehandlingPathParam
import no.nav.aap.tilgang.Operasjon
import no.nav.aap.tilgang.authorizedGet
import javax.sql.DataSource


class TrukketSøknadGrunnlagDto(
    val vurderinger: List<TrukketSøknadVurderingDto>

)

class TrukketSøknadVurderingDto(
    val journalpostId: String,
    val begrunnelse: String,
    val skalTrekkes: Boolean,
    val vurderingerMeta: VurderingerMetaResponse,
)

fun NormalOpenAPIRoute.trukketSøknadGrunnlagApi(
    dataSource: DataSource,
    repositoryRegistry: RepositoryRegistry,
    gatewayProvider: GatewayProvider,
) {
    route("/api/behandling/{referanse}/grunnlag/trukket-søknad").authorizedGet<BehandlingReferanse, TrukketSøknadGrunnlagDto>(
        AuthorizationParamPathConfig(
            relevanteIdenterResolver = relevanteIdenterForBehandlingResolver(repositoryRegistry, dataSource),
            operasjon = Operasjon.SE,
            behandlingPathParam = BehandlingPathParam("referanse")
        )
    ) { req ->
        val trukketSøknadVurderingDto = dataSource.transaction(readOnly = true) { connection ->
            val repositoryProvider = repositoryRegistry.provider(connection)
            val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
            val trukketSøknadRepository = repositoryProvider.provide<TrukketSøknadRepository>()
            val vurdertAvService = VurdertAvService(repositoryProvider, gatewayProvider)

            val behandlingId = behandlingRepository.hent(BehandlingReferanse(req.referanse)).id
            TrukketSøknadGrunnlagDto(
                vurderinger = trukketSøknadRepository.hentTrukketSøknadVurderinger(behandlingId)
                    .map {
                        TrukketSøknadVurderingDto(
                            journalpostId = it.journalpostId.identifikator,
                            begrunnelse = it.begrunnelse,
                            skalTrekkes = it.skalTrekkes,
                            vurderingerMeta = vurdertAvService.byggVurderingerMeta(
                                definisjon = Definisjon.VURDER_TREKK_AV_SØKNAD,
                                behandlingId = behandlingId,
                                vurdertAv = vurdertAvService.medNavnOgEnhet(it.vurdertAv.ident, it.vurdert),
                            ),
                        )
                    }
            )
        }
        respond(trukketSøknadVurderingDto)
    }
}
