package no.nav.aap.behandlingsflyt.behandling.vedtakslengde

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvService
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.vedtakslengde.VedtakslengdeRepository
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
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.tilgang.BehandlingPathParam
import no.nav.aap.tilgang.getGrunnlag
import javax.sql.DataSource

fun NormalOpenAPIRoute.vedtakslengdeGrunnlagApi(
    dataSource: DataSource,
    repositoryRegistry: RepositoryRegistry,
    gatewayProvider: GatewayProvider
) {
    route("/api/behandling/{referanse}/grunnlag/vedtakslengde") {
        getGrunnlag<BehandlingReferanse, VedtakslengdeGrunnlagResponse>(
            relevanteIdenterResolver = relevanteIdenterForBehandlingResolver(repositoryRegistry, dataSource),
            behandlingPathParam = BehandlingPathParam("referanse"),
            avklaringsbehovKode = Definisjon.AVKLAR_VEDTAKSLENGDE.kode.toString(),
        ) { req ->
            val vedtakslengdeGrunnlag =
                dataSource.transaction(readOnly = true) { connection ->
                    val repositoryProvider = repositoryRegistry.provider(connection)
                    val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                    val vedtakslengdeRepository = repositoryProvider.provide<VedtakslengdeRepository>()
                    val sakRepository = repositoryProvider.provide<SakRepository>()

                    val behandling: Behandling =
                        BehandlingReferanseService(behandlingRepository).behandling(req)
                    val sak = sakRepository.hent(behandling.sakId)
                    val vurdertAvService = VurdertAvService(repositoryProvider, gatewayProvider)

                    val grunnlag = vedtakslengdeRepository.hentHvisEksisterer(behandling.id)
                    val vedtattGrunnlag = behandling.forrigeBehandlingId?.let { vedtakslengdeRepository.hentHvisEksisterer(it) }
                    val nyeVurderinger = grunnlag?.vurderinger?.filter { it.vurdertIBehandling == behandling.id } ?: emptyList()

                    val initiellStartdato = sak.rettighetsperiode.fom
                    val sluttdatoGjeldendeVurdering = vedtattGrunnlag?.gjeldendeVurdering()?.sluttdato

                    // Startdato i nye vurderinger fortsetter fra forrige vedtatte grunnlag
                    val startdatoNyeVurderinger = sluttdatoGjeldendeVurdering?.plusDays(1) ?: initiellStartdato

                    VedtakslengdeGrunnlagResponse(
                        harTilgangTilÅSaksbehandle = kanSaksbehandle(),
                        kanVurderes = listOf(sak.rettighetsperiode),
                        behøverVurderinger = emptyList(),
                        nyeVurderinger = nyeVurderinger.map { it.toResponse(vurdertAvService,
                            // Bruker gjeldende vedtatt sluttdato som startdato dersom denne er lik sluttdato i ny vurdering
                            Periode(if (sluttdatoGjeldendeVurdering == it.sluttdato) it.sluttdato else startdatoNyeVurderinger, it.sluttdato)
                        ) },
                        sisteVedtatteVurderinger = vedtattGrunnlag
                            ?.gjeldendeVurderinger(initiellStartdato)
                            ?.segmenter()
                            ?.map { it.verdi.toResponse(vurdertAvService, it.periode) }
                            ?: emptyList(),
                        ikkeRelevantePerioder = emptyList(),
                    )
                }

            respond(vedtakslengdeGrunnlag)
        }
    }
}
