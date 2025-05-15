package no.nav.aap.behandlingsflyt.behandling.rettighetsperiode

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.behandling.lovvalgmedlemskap.grunnlag.VurdertAvResponse
import no.nav.aap.behandlingsflyt.behandling.søknad.SøknadsdatoUtleder
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.tilgang.TilgangGatewayImpl
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.httpklient.auth.token
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.tilgang.AuthorizationParamPathConfig
import no.nav.aap.tilgang.BehandlingPathParam
import no.nav.aap.tilgang.Operasjon
import no.nav.aap.tilgang.authorizedGet
import java.time.LocalDate
import javax.sql.DataSource


class RettighetsperiodeGrunnlagResponse(
    val vurdering: RettighetsperiodeVurderingResponse?,
    val søknadsdato: LocalDate?,
    val harTilgangTilÅSaksbehandle: Boolean
)

class RettighetsperiodeVurderingResponse(
    val begrunnelse: String,
    val harRettUtoverSøknadsdato: Boolean,
    val startDato: LocalDate?,
    val harKravPåRenter: Boolean?,
    val vurdertAv: VurdertAvResponse
)


fun NormalOpenAPIRoute.rettighetsperiodeGrunnlagAPI(dataSource: DataSource, repositoryRegistry: RepositoryRegistry) {
    route("/api/behandling/{referanse}/grunnlag/rettighetsperiode").authorizedGet<BehandlingReferanse, RettighetsperiodeGrunnlagResponse>(
        AuthorizationParamPathConfig(
            operasjon = Operasjon.SE,
            behandlingPathParam = BehandlingPathParam("referanse")
        )
    ) { req ->
        val rettighetsperiodeGrunnlagDto = dataSource.transaction(readOnly = true) { connection ->
            val repositoryProvider = repositoryRegistry.provider(connection)
            val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
            val rettighetsperiodeRepository = repositoryProvider.provide<VurderRettighetsperiodeRepository>()
            val mottattDokumentRepository = repositoryProvider.provide<MottattDokumentRepository>()
            val søknadsdatoUtleder = SøknadsdatoUtleder(mottattDokumentRepository)
            val hartilgangTilÅSaksbehandle = TilgangGatewayImpl.sjekkTilgangTilBehandling(
                req.referanse,
                Definisjon.VURDER_RETTIGHETSPERIODE.kode.toString(),
                token()
            )

            val behandling = behandlingRepository.hent(BehandlingReferanse(req.referanse))
            RettighetsperiodeGrunnlagResponse(
                vurdering =
                    rettighetsperiodeRepository.hentVurdering(behandling.id)?.let {
                        RettighetsperiodeVurderingResponse(
                            begrunnelse = it.begrunnelse,
                            startDato = it.startDato,
                            harRettUtoverSøknadsdato = it.harRettUtoverSøknadsdato,
                            harKravPåRenter = it.harKravPåRenter,
                            vurdertAv =
                                VurdertAvResponse(
                                    ident = it.vurdertAv,
                                    dato =
                                        it.vurdertDato?.toLocalDate()
                                            ?: error("Mangler vurdertdato på rettighetsperiodevurderingen")
                                )
                        )
                    },
                søknadsdato = søknadsdatoUtleder.utledSøknadsdatoForSak(behandling.sakId)?.toLocalDate(),
                harTilgangTilÅSaksbehandle = hartilgangTilÅSaksbehandle
            )
        }
        respond(rettighetsperiodeGrunnlagDto)
    }
}
