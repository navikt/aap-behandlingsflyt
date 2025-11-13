package no.nav.aap.behandlingsflyt.behandling.arbeidsopptrapping

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.behandling.ansattinfo.AnsattInfoService
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvResponse
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsopptrapping.ArbeidsopptrappingRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsopptrapping.ArbeidsopptrappingVurdering
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.behandlingsflyt.tilgang.kanSaksbehandle
import no.nav.aap.behandlingsflyt.tilgang.relevanteIdenterForBehandlingResolver
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.tilgang.BehandlingPathParam
import no.nav.aap.tilgang.getGrunnlag
import javax.sql.DataSource

fun NormalOpenAPIRoute.arbeidsopptrappingGrunnlagApi(
    dataSource: DataSource,
    repositoryRegistry: RepositoryRegistry,
    gatewayProvider: GatewayProvider,
) {
    val ansattInfoService = AnsattInfoService(gatewayProvider)
    val unleashGateway = gatewayProvider.provide<UnleashGateway>()

    route("/api/behandling/{referanse}/grunnlag/arbeidsopptrapping") {
        getGrunnlag<BehandlingReferanse, ArbeidsopptrappingGrunnlagResponse>(
            relevanteIdenterResolver = relevanteIdenterForBehandlingResolver(repositoryRegistry, dataSource),
            behandlingPathParam = BehandlingPathParam("referanse"),
            avklaringsbehovKode = Definisjon.ARBEIDSOPPTRAPPING.kode.toString()
        ) { behandlingReferanse ->
            val response = if (unleashGateway.isEnabled(BehandlingsflytFeature.Arbeidsopptrapping)) {
                arbeidsopptrappingGrunnlag(
                    dataSource,
                    behandlingReferanse,
                    kanSaksbehandle(),
                    repositoryRegistry,
                    ansattInfoService
                )
            } else {
                ArbeidsopptrappingGrunnlagResponse(
                    kanSaksbehandle(), emptyList(), emptyList(), emptySet(), emptyList()
                )
            }

            respond(
                response
            )
        }
    }
}

private fun arbeidsopptrappingGrunnlag(
    dataSource: DataSource,
    behandlingReferanse: BehandlingReferanse,
    kanSaksbehandle: Boolean,
    repositoryRegistry: RepositoryRegistry,
    ansattInfoService: AnsattInfoService,
): ArbeidsopptrappingGrunnlagResponse {
    return dataSource.transaction { connection ->
        val repositoryProvider = repositoryRegistry.provider(connection)
        val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
        val behandling: Behandling =
            BehandlingReferanseService(behandlingRepository).behandling(behandlingReferanse)
        val arbeidsopptrappingRepository = repositoryProvider.provide<ArbeidsopptrappingRepository>()

        val nåTilstand = arbeidsopptrappingRepository.hentHvisEksisterer(behandling.id)?.vurderinger

        val vedtatteVerdier =
            behandling.forrigeBehandlingId?.let { arbeidsopptrappingRepository.hentHvisEksisterer(it) }?.vurderinger.orEmpty()

        val historikk = arbeidsopptrappingRepository.hentAlleVurderinger(behandling.sakId, behandling.id)

        ArbeidsopptrappingGrunnlagResponse(
            harTilgangTilÅSaksbehandle = kanSaksbehandle,
            historikk =
                historikk
                    .map { tilResponse(it, ansattInfoService) }
                    .sortedBy { it.vurderingsTidspunkt }
                    .toSet(),
            gjeldendeVedtatteVurderinger =
                vedtatteVerdier
                    .map { tilResponse(it, ansattInfoService) }
                    .sortedBy { it.fraDato },
            vurderinger =
                nåTilstand
                    ?.filterNot { vedtatteVerdier.contains(it) }
                    ?.map { tilResponse(it, ansattInfoService) }
                    ?.sortedBy { it.fraDato }
                    .orEmpty(),
            perioderSomKanVurderes = listOf()
        )
    }
}

private fun tilResponse(
    arbeidsopptrappingVurdering: ArbeidsopptrappingVurdering,
    ansattInfoService: AnsattInfoService
): ArbeidsopptrappingVurderingResponse {
    val ansattNavnOgEnhet = ansattInfoService.hentAnsattNavnOgEnhet(arbeidsopptrappingVurdering.vurdertAv)

    return ArbeidsopptrappingVurderingResponse(
        begrunnelse = arbeidsopptrappingVurdering.begrunnelse,
        reellMulighetTilOpptrapping = arbeidsopptrappingVurdering.reellMulighetTilOpptrapping,
        rettPaaAAPIOpptrapping = arbeidsopptrappingVurdering.rettPaaAAPIOpptrapping,
        fraDato = arbeidsopptrappingVurdering.fraDato,
        vurdertAv = VurdertAvResponse(
            ident = arbeidsopptrappingVurdering.vurdertAv,
            dato = arbeidsopptrappingVurdering.opprettetTid?.toLocalDate()
                ?: error("Fant ikke opprettet tidspunkt for arbeidsopptrappingsvurdering"),
            ansattnavn = ansattNavnOgEnhet?.navn,
            enhetsnavn = ansattNavnOgEnhet?.enhet
        ),
        vurderingsTidspunkt = arbeidsopptrappingVurdering.opprettetTid
            ?: error("Fant ikke opprettet tidspunkt for arbeidsopptrappingsvurdering"),
    )
}