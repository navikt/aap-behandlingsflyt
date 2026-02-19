package no.nav.aap.behandlingsflyt.behandling.tidligerevurderinger

import com.fasterxml.jackson.annotation.JsonValue
import com.papsign.ktor.openapigen.annotations.parameters.PathParam
import com.papsign.ktor.openapigen.annotations.parameters.QueryParam
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.periodisering.FlytKontekstMedPeriodeService
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.behandlingsflyt.tilgang.relevanteIdenterForBehandlingResolver
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.tilgang.AuthorizationParamPathConfig
import no.nav.aap.tilgang.BehandlingPathParam
import no.nav.aap.tilgang.authorizedGet
import java.util.UUID
import javax.sql.DataSource

fun NormalOpenAPIRoute.tidligereVurderingerApi(
    dataSource: DataSource,
    repositoryRegistry: RepositoryRegistry,
    gatewayProvider: GatewayProvider
) {
    route("/api/behandling") {
        route("/{referanse}/tidligere-vurderinger").authorizedGet<TidligereVurderingerReq, TidligereVurderingerDto>(
            AuthorizationParamPathConfig(
                relevanteIdenterResolver = relevanteIdenterForBehandlingResolver(repositoryRegistry, dataSource),
                behandlingPathParam = BehandlingPathParam(
                    "referanse"
                )
            )
        ) { req ->
            val response = dataSource.transaction(readOnly = true) { connection ->
                val repositoryProvider = repositoryRegistry.provider(connection)

                val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                val behandling =
                    BehandlingReferanseService(behandlingRepository).behandling(BehandlingReferanse(req.referanse))

                val kontekst = FlytKontekstMedPeriodeService(repositoryProvider, gatewayProvider).utled(
                    behandling.flytKontekst(),
                    req.stegType
                )

                val tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider)
                TidligereVurderingerDto(tidligereVurderinger.behandlingsutfall(kontekst, req.stegType).segmenter().map {
                    val verdi = it.verdi
                    TidligereVurderingDto(
                        periode = it.periode,
                        utfall = BehandlingsutfallType.fraBehandlingsutfall(verdi),
                        rettighetstype = when (verdi) {
                            is TidligereVurderinger.PotensieltOppfylt -> verdi.rettighetstype
                            else -> null
                        }
                    )
                })
            }

            respond(response)
        }
    }
}


data class TidligereVurderingerReq(
    @JsonValue @param:PathParam("referanse") val referanse: UUID = UUID.randomUUID(),
    @param:QueryParam("Tidligere vurderinger frem til steg") val stegType: StegType
)

data class TidligereVurderingerDto(
    val tidligereVurderinger: List<TidligereVurderingDto>
)

data class TidligereVurderingDto(
    val periode: Periode,
    val utfall: BehandlingsutfallType,
    val rettighetstype: RettighetsType?
)

enum class BehandlingsutfallType {
    IKKE_BEHANDLINGSGRUNNLAG,
    UUNGÅELIG_AVSLAG,
    POTENSIELT_OPPFYLT,
    UKJENT;

    companion object {
        fun fraBehandlingsutfall(behandlingsutfall: TidligereVurderinger.Behandlingsutfall): BehandlingsutfallType =
            when (behandlingsutfall) {
                TidligereVurderinger.IkkeBehandlingsgrunnlag -> IKKE_BEHANDLINGSGRUNNLAG
                TidligereVurderinger.UunngåeligAvslag -> UUNGÅELIG_AVSLAG
                is TidligereVurderinger.PotensieltOppfylt -> POTENSIELT_OPPFYLT
            }
    }
}