package no.nav.aap.behandlingsflyt.behandling.søknad

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.tilgang.relevanteIdenterForBehandlingResolver
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.tilgang.AuthorizationParamPathConfig
import no.nav.aap.tilgang.BehandlingPathParam
import no.nav.aap.tilgang.Operasjon
import no.nav.aap.tilgang.authorizedGet
import java.time.LocalDate
import java.time.ZoneId
import javax.sql.DataSource


class TrukketSøknadGrunnlagDto(
    val vurderinger: List<TrukketSøknadVurderingDto>

)

class TrukketSøknadVurderingDto(
    val journalpostId: String,
    val begrunnelse: String,
    val vurdertDato: LocalDate,
    val vurdertAv: String,
)

fun NormalOpenAPIRoute.trukketSøknadGrunnlagAPI(dataSource: DataSource, repositoryRegistry: RepositoryRegistry) {
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

            val behandlingId = behandlingRepository.hent(BehandlingReferanse(req.referanse)).id
            TrukketSøknadGrunnlagDto(
                vurderinger = trukketSøknadRepository.hentTrukketSøknadVurderinger(behandlingId)
                    .map {
                        TrukketSøknadVurderingDto(
                            journalpostId = it.journalpostId.identifikator,
                            begrunnelse = it.begrunnelse,
                            vurdertAv = it.vurdertAv.ident,
                            vurdertDato = it.vurdert.atZone(ZoneId.of("Europe/Oslo")).toLocalDate(),
                        )
                    }
            )
        }
        respond(trukketSøknadVurderingDto)
    }
}
