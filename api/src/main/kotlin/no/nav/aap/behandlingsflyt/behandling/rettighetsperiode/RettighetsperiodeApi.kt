package no.nav.aap.behandlingsflyt.behandling.rettighetsperiode

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.tilgang.AuthorizationParamPathConfig
import no.nav.aap.tilgang.BehandlingPathParam
import no.nav.aap.tilgang.Operasjon
import no.nav.aap.tilgang.authorizedGet
import java.time.LocalDate
import javax.sql.DataSource


class RettighetsperiodeGrunnlagDto(
    val vurderinger: List<RettighetsperiodeVurderingDto>

)

class RettighetsperiodeVurderingDto(
    val begrunnelse: String,
    val startDato: LocalDate,
    val harRettUtoverSøknadsdato: Boolean,
    val harKravPåRenter: Boolean,
)


fun NormalOpenAPIRoute.rettighetsperiodeGrunnlagAPI(dataSource: DataSource) {
    route("/api/behandling/{referanse}/grunnlag/rettighetsperiode").authorizedGet<BehandlingReferanse, RettighetsperiodeGrunnlagDto>(
        AuthorizationParamPathConfig(
            operasjon = Operasjon.SE,
            behandlingPathParam = BehandlingPathParam("referanse")
        )
    ) { req ->
        val trukketSøknadVurderingDto = dataSource.transaction(readOnly = true) { connection ->
            val repositoryProvider = RepositoryProvider(connection)
            val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
            val rettighetsperiodeRepository = repositoryProvider.provide<VurderRettighetsperiodeRepository>()

            val behandlingId = behandlingRepository.hent(BehandlingReferanse(req.referanse)).id
            RettighetsperiodeGrunnlagDto(
                vurderinger = rettighetsperiodeRepository.hentVurderinger(behandlingId)
                    .map {
                        RettighetsperiodeVurderingDto(
                            begrunnelse = it.begrunnelse,
                            startDato = it.startDato,
                            harRettUtoverSøknadsdato = it.harRettUtoverSøknadsdato,
                            harKravPåRenter = it.harKravPåRenter,
                        )
                    }
            )
        }
        respond(trukketSøknadVurderingDto)
    }
}
