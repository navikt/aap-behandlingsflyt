package no.nav.aap.behandlingsflyt.behandling.beregning.grunnlag.fritakmeldeplikt

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvService
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.MeldepliktRepository
import no.nav.aap.behandlingsflyt.harTilgangOgKanSaksbehandle
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.tilgang.kanSaksbehandle
import no.nav.aap.behandlingsflyt.tilgang.relevanteIdenterForBehandlingResolver
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.komponenter.tidslinje.orEmpty
import no.nav.aap.tilgang.BehandlingPathParam
import no.nav.aap.tilgang.getGrunnlag
import javax.sql.DataSource

fun NormalOpenAPIRoute.meldepliktsgrunnlagApi(
    dataSource: DataSource,
    repositoryRegistry: RepositoryRegistry,
    gatewayProvider: GatewayProvider
) {
    route("/api/behandling/{referanse}/grunnlag/fritak-meldeplikt") {
        getGrunnlag<BehandlingReferanse, FritakMeldepliktGrunnlagResponse>(
            relevanteIdenterResolver = relevanteIdenterForBehandlingResolver(repositoryRegistry, dataSource),
            behandlingPathParam = BehandlingPathParam("referanse"),
            avklaringsbehovKode = Definisjon.FRITAK_MELDEPLIKT.kode.toString(),
        ) { req ->
            val meldepliktGrunnlag =
                dataSource.transaction(readOnly = true) { connection ->
                    val repositoryProvider = repositoryRegistry.provider(connection)
                    val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                    val meldepliktRepository = repositoryProvider.provide<MeldepliktRepository>()
                    val sakRepository = repositoryProvider.provide<SakRepository>()

                    val behandling: Behandling =
                        BehandlingReferanseService(behandlingRepository).behandling(req)
                    val sak = sakRepository.hent(behandling.sakId)
                    val vurdertAvService = VurdertAvService(repositoryProvider, gatewayProvider)

                    val nåTilstand = meldepliktRepository.hentHvisEksisterer(behandling.id)?.vurderinger
                    val forrigeGrunnlag = behandling.forrigeBehandlingId?.let { meldepliktRepository.hentHvisEksisterer(it) }
                    val nyeVurderinger = nåTilstand?.filter { it.vurdertIBehandling == behandling.id } ?: emptyList()

                    val avklaringsbehovRepository = repositoryProvider.provide<AvklaringsbehovRepository>()
                    val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(behandling.id)

                    FritakMeldepliktGrunnlagResponse(
                        harTilgangTilÅSaksbehandle = harTilgangOgKanSaksbehandle(kanSaksbehandle(), avklaringsbehovene),
                        kanVurderes = listOf(sak.rettighetsperiode),
                        behøverVurderinger = emptyList(),
                        nyeVurderinger = nyeVurderinger.map { it.toResponse(vurdertAvService) },
                        sisteVedtatteVurderinger = forrigeGrunnlag?.gjeldendeVurderinger().orEmpty()
                            .toResponse(vurdertAvService),
                        ikkeRelevantePerioder = emptyList(/* Dette avklaringsbehovet er frivillig, og vi har per nå ikke disse opplysningene lett tilgjengelig. */),
                    )
                }

            respond(meldepliktGrunnlag)
        }
    }
}