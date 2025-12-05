package no.nav.aap.behandlingsflyt.behandling.beregning.grunnlag.sykdom.sykepengergrunnlag

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvService
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykepengerErstatningRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykepengerVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.somTidslinje
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
import no.nav.aap.tilgang.BehandlingPathParam
import no.nav.aap.tilgang.getGrunnlag
import javax.sql.DataSource
import kotlin.collections.orEmpty


fun NormalOpenAPIRoute.sykepengerGrunnlagApi(
    dataSource: DataSource,
    repositoryRegistry: RepositoryRegistry,
    gatewayProvider: GatewayProvider,
) {
    route("/api/behandling") {
        route("/{referanse}/grunnlag/sykdom/sykepengergrunnlag") {
            getGrunnlag<BehandlingReferanse, SykepengerGrunnlagResponse>(
                relevanteIdenterResolver = relevanteIdenterForBehandlingResolver(repositoryRegistry, dataSource),
                behandlingPathParam = BehandlingPathParam("referanse"),
                avklaringsbehovKode = Definisjon.AVKLAR_SYKEPENGEERSTATNING.kode.toString()
            ) { req ->
                val response = dataSource.transaction(readOnly = true) { connection ->
                    val repositoryProvider = repositoryRegistry.provider(connection)
                    val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                    val sykepengerErstatningRepository = repositoryProvider.provide<SykepengerErstatningRepository>()
                    val avklaringsbehovRepository = repositoryProvider.provide<AvklaringsbehovRepository>()
                    val sakRepository = repositoryProvider.provide<SakRepository>()
                    val vurdertAvService = VurdertAvService(repositoryProvider, gatewayProvider)
                    val behandling: Behandling = BehandlingReferanseService(behandlingRepository).behandling(req)

                    val grunnlag = sykepengerErstatningRepository.hentHvisEksisterer(behandling.id)
                    val sak = sakRepository.hent(behandling.sakId)
                    val vedtatteVurderinger = grunnlag?.vurderinger?.filter { it.vurdertIBehandling != behandling.id }.orEmpty()
                    val nyeVurderinger = grunnlag?.vurderinger?.filter { it.vurdertIBehandling == behandling.id }.orEmpty()

                    val avklaringsbehov = avklaringsbehovRepository.hentAvklaringsbehovene(behandling.id)
                    val kanVurderes = listOf(sak.rettighetsperiode)
                    val perioderSomTrengerVurdering = avklaringsbehov.hentBehovForDefinisjon(Definisjon.AVKLAR_SYKEPENGEERSTATNING)?.perioderVedtaketBehøverVurdering().orEmpty()

                    val sisteVedtatteVurderinger = vedtatteVurderinger
                        .somTidslinje()
                        .segmenter()
                        .map { it.verdi.tilResponse(vurdertAvService) }

                    SykepengerGrunnlagResponse(
                        harTilgangTilÅSaksbehandle = kanSaksbehandle(),
                        vurderinger = nyeVurderinger.map { it.tilResponse(vurdertAvService) },
                        vedtatteVurderinger = vedtatteVurderinger.map { it.tilResponse(vurdertAvService) },
                        sisteVedtatteVurderinger = sisteVedtatteVurderinger,
                        nyeVurderinger = nyeVurderinger.map { it.tilResponse(vurdertAvService) },
                        kanVurderes = kanVurderes,
                        behøverVurderinger = perioderSomTrengerVurdering.toList()
                    )
                }
                respond(response)
            }
        }
    }
}

private fun SykepengerVurdering.tilResponse(vurdertAvService: VurdertAvService): SykepengerVurderingResponse {
    return SykepengerVurderingResponse(
        begrunnelse = begrunnelse,
        dokumenterBruktIVurdering = dokumenterBruktIVurdering,
        harRettPå = harRettPå,
        grunn = grunn,
        fom = gjelderFra,
        tom = gjelderTom,
        gjelderFra = gjelderFra,
        gjelderTom = gjelderTom,
        opprettet = vurdertTidspunkt ?: error("Mangler dato for sykepengervurdering") ,
        vurdertIBehandling = vurdertIBehandling,
        besluttetAv = vurdertAvService.besluttetAv(Definisjon.AVKLAR_SYKEPENGEERSTATNING, vurdertIBehandling),
        kvalitetssikretAv = vurdertAvService.kvalitetssikretAv(Definisjon.AVKLAR_SYKEPENGEERSTATNING, vurdertIBehandling),
        vurdertAv = vurdertAvService.medNavnOgEnhet(ident = vurdertAv, vurdertTidspunkt ?: error("Mangler dato for sykepengervurdering"))
    )
}

