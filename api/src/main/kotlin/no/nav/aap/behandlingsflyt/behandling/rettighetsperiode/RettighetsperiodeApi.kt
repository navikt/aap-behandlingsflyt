package no.nav.aap.behandlingsflyt.behandling.rettighetsperiode

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.behandling.søknad.SøknadsdatoUtleder
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepository
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.tilgang.AuthorizationParamPathConfig
import no.nav.aap.tilgang.BehandlingPathParam
import no.nav.aap.tilgang.Operasjon
import no.nav.aap.tilgang.authorizedGet
import java.time.LocalDate
import javax.sql.DataSource


class RettighetsperiodeGrunnlagDto(
    val vurdering: RettighetsperiodeVurderingDto?,
    val søknadsdato: LocalDate?,
)

class RettighetsperiodeVurderingDto(
    val begrunnelse: String,
    val harRettUtoverSøknadsdato: Boolean,
    val startDato: LocalDate?,
    val harKravPåRenter: Boolean?,
)


fun NormalOpenAPIRoute.rettighetsperiodeGrunnlagAPI(dataSource: DataSource, repositoryRegistry: RepositoryRegistry) {
    route("/api/behandling/{referanse}/grunnlag/rettighetsperiode").authorizedGet<BehandlingReferanse, RettighetsperiodeGrunnlagDto>(
        AuthorizationParamPathConfig(
            operasjon = Operasjon.SE,
            behandlingPathParam = BehandlingPathParam("referanse")
        )
    ) { req ->
        val trukketSøknadVurderingDto = dataSource.transaction(readOnly = true) { connection ->
            val repositoryProvider = repositoryRegistry.provider(connection)
            val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
            val rettighetsperiodeRepository = repositoryProvider.provide<VurderRettighetsperiodeRepository>()
            val mottattDokumentRepository = repositoryProvider.provide<MottattDokumentRepository>()
            val søknadsdatoUtleder = SøknadsdatoUtleder(mottattDokumentRepository)

            val behandling = behandlingRepository.hent(BehandlingReferanse(req.referanse))
            RettighetsperiodeGrunnlagDto(
                vurdering = rettighetsperiodeRepository.hentVurdering(behandling.id)?.let {
                    RettighetsperiodeVurderingDto(
                        begrunnelse = it.begrunnelse,
                        startDato = it.startDato,
                        harRettUtoverSøknadsdato = it.harRettUtoverSøknadsdato,
                        harKravPåRenter = it.harKravPåRenter,
                    )
                },
                søknadsdato = søknadsdatoUtleder.utledSøknadsdatoForSak(behandling.sakId)?.toLocalDate()
            )
        }
        respond(trukketSøknadVurderingDto)
    }
}
