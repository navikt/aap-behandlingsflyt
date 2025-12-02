package no.nav.aap.behandlingsflyt.behandling.oppholdskrav

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.behandling.ansattinfo.AnsattInfoService
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvResponse
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
import no.nav.aap.komponenter.verdityper.Tid
import no.nav.aap.tilgang.BehandlingPathParam
import no.nav.aap.tilgang.getGrunnlag
import javax.sql.DataSource

fun NormalOpenAPIRoute.oppholdskravGrunnlagApi(
    dataSource: DataSource,
    repositoryRegistry: RepositoryRegistry,
    gatewayProvider: GatewayProvider,
) {
    val ansattInfoService = AnsattInfoService(gatewayProvider)

    route("/api/behandling/{referanse}/grunnlag/oppholdskrav") {
        getGrunnlag<BehandlingReferanse, OppholdskravGrunnlagResponse>(
            relevanteIdenterResolver = relevanteIdenterForBehandlingResolver(repositoryRegistry, dataSource),
            behandlingPathParam = BehandlingPathParam("referanse"),
            avklaringsbehovKode = Definisjon.AVKLAR_OPPHOLDSKRAV.kode.toString(),
        ) { req ->
            val oppholdskravGrunnlag =
                dataSource.transaction(readOnly = true) { connection ->
                    val repositoryProvider = repositoryRegistry.provider(connection)
                    val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                    val oppholdskravRepository = repositoryProvider.provide<OppholdskravGrunnlagRepository>()
                    val sakRepository = repositoryProvider.provide<SakRepository>()
                    val avklaringsbehovRepository = repositoryProvider.provide<AvklaringsbehovRepository>()

                    val behandling: Behandling = BehandlingReferanseService(behandlingRepository).behandling(req)
                    val sak = sakRepository.hent(behandling.sakId)
                    val grunnlag = oppholdskravRepository.hentHvisEksisterer(behandling.id)
                    val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(behandling.id)
                    val avklaringsbehov = avklaringsbehovene.hentBehovForDefinisjon(Definisjon.AVKLAR_OPPHOLDSKRAV)

                    val vurdering = grunnlag?.vurderinger?.firstOrNull { it.vurdertIBehandling == behandling.id }
                    val gjeldendeVedtatteVurderinger = grunnlag?.vurderinger?.filter { it.vurdertIBehandling != behandling.id }?.tilTidslinje().orEmpty()
                    val perioderSomTrengerVurdering = avklaringsbehov?.perioderSomSkalLøses()?.toList() ?: emptyList()

                    OppholdskravGrunnlagResponse(
                        harTilgangTilÅSaksbehandle = kanSaksbehandle(),
                        behøverVurderinger = perioderSomTrengerVurdering,
                        kanVurderes = listOf(sak.rettighetsperiode),
                        nyeVurderinger = vurdering?.tilDto(ansattInfoService) ?: emptyList(),
                        sisteVedtatteVurderinger = gjeldendeVedtatteVurderinger
                            .komprimer()
                            .segmenter()
                            .map { segment ->
                                OppholdskravVurderingDto(
                                    vurdertAv = VurdertAvResponse.fraIdent(segment.verdi.vurdertAv, segment.verdi.opprettet.toLocalDate(), ansattInfoService),
                                    fom = segment.fom(),
                                    tom = if (segment.tom().isEqual( Tid.MAKS)) null else segment.tom(),
                                    begrunnelse = segment.verdi.begrunnelse,
                                    land = segment.verdi.land,
                                    oppfylt = segment.verdi.oppfylt,
                                )
                            }
                    )
                }
            respond(oppholdskravGrunnlag)
        }
    }
}
