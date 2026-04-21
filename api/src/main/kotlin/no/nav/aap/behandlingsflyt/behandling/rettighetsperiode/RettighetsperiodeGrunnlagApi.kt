package no.nav.aap.behandlingsflyt.behandling.rettighetsperiode

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.behandling.søknad.DatoFraDokumentUtleder
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvService
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.tilgang.kanSaksbehandle
import no.nav.aap.behandlingsflyt.tilgang.relevanteIdenterForBehandlingResolver
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.tilgang.BehandlingPathParam
import no.nav.aap.tilgang.getGrunnlag
import javax.sql.DataSource

fun NormalOpenAPIRoute.rettighetsperiodeGrunnlagApi(
    dataSource: DataSource,
    repositoryRegistry: RepositoryRegistry,
    gatewayProvider: GatewayProvider,
) {
    route("/api/behandling/{referanse}/grunnlag/rettighetsperiode")
        .getGrunnlag<BehandlingReferanse, RettighetsperiodeGrunnlagResponse>(
            relevanteIdenterResolver = relevanteIdenterForBehandlingResolver(repositoryRegistry, dataSource),
            behandlingPathParam = BehandlingPathParam("referanse"),
            avklaringsbehovKode = Definisjon.VURDER_RETTIGHETSPERIODE.kode.toString()

        ) { req ->
            val rettighetsperiodeGrunnlagDto = dataSource.transaction(readOnly = true) { connection ->
                val repositoryProvider = repositoryRegistry.provider(connection)
                val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                val rettighetsperiodeRepository = repositoryProvider.provide<VurderRettighetsperiodeRepository>()
                val mottattDokumentRepository = repositoryProvider.provide<MottattDokumentRepository>()
                val datoFraDokumentUtleder = DatoFraDokumentUtleder(mottattDokumentRepository)

                val behandling = behandlingRepository.hent(BehandlingReferanse(req.referanse))
                val vurdering = rettighetsperiodeRepository.hentVurdering(behandling.id)

                val vurdertAvService = VurdertAvService(repositoryProvider, gatewayProvider)

                RettighetsperiodeGrunnlagResponse(
                    vurdering = vurdering?.tilDto(vurdertAvService, behandling.id),
                    søknadsdato = datoFraDokumentUtleder.utledSøknadsdatoForSak(behandling.sakId)?.toLocalDate(),
                    harTilgangTilÅSaksbehandle = kanSaksbehandle()
                )
            }
            respond(rettighetsperiodeGrunnlagDto)
        }
}
