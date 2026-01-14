package no.nav.aap.behandlingsflyt.behandling.beregning.grunnlag.fritakmeldeplikt

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.behandling.ansattinfo.AnsattInfoService
import no.nav.aap.behandlingsflyt.behandling.lovvalg.tilTidslinje
import no.nav.aap.behandlingsflyt.behandling.lovvalgmedlemskap.grunnlag.PeriodisertManuellVurderingForForutgåendeMedlemskapResponse
import no.nav.aap.behandlingsflyt.behandling.lovvalgmedlemskap.grunnlag.toResponse
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvResponse
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvService
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.ManuellVurderingForForutgåendeMedlemskap
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.Fritaksvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.MeldepliktRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.tilTidslinje
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
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.orEmpty
import no.nav.aap.komponenter.verdityper.Tid
import no.nav.aap.tilgang.BehandlingPathParam
import no.nav.aap.tilgang.getGrunnlag
import java.time.LocalDate
import java.time.LocalDateTime
import javax.sql.DataSource

fun NormalOpenAPIRoute.meldepliktsgrunnlagApi(
    dataSource: DataSource,
    repositoryRegistry: RepositoryRegistry,
    gatewayProvider: GatewayProvider
) {
    val ansattInfoService = AnsattInfoService(gatewayProvider)

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
                    val forrigeGrunnlag =
                        behandling.forrigeBehandlingId?.let { meldepliktRepository.hentHvisEksisterer(it) }

                    val vedtatteVerdier = forrigeGrunnlag?.vurderinger.orEmpty()

                    val historikk =
                        meldepliktRepository.hentAlleVurderinger(behandling.sakId, behandling.id)

                    val nyeVurderinger = nåTilstand?.filter { it.vurdertIBehandling == behandling.id } ?: emptyList()

                    FritakMeldepliktGrunnlagResponse(
                        harTilgangTilÅSaksbehandle = kanSaksbehandle(),
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
                        kanVurderes = listOf(sak.rettighetsperiode),
                        behøverVurderinger = emptyList(),
                        nyeVurderinger = nyeVurderinger.map { it.toResponse(vurdertAvService) },
                        sisteVedtatteVurderinger = forrigeGrunnlag?.gjeldendeVurderinger().orEmpty()
                            .toResponse(vurdertAvService),
                        kvalitetssikretAv = vurdertAvService.kvalitetssikretAv(
                            Definisjon.FRITAK_MELDEPLIKT,
                            behandling.id
                        )
                    )
                }

            respond(meldepliktGrunnlag)
        }
    }
}

private fun tilResponse(
    fritaksvurdering: Fritaksvurdering,
    ansattInfoService: AnsattInfoService
): FritakMeldepliktVurderingResponse {
    val ansattNavnOgEnhet = ansattInfoService.hentAnsattNavnOgEnhet(fritaksvurdering.vurdertAv)

    return FritakMeldepliktVurderingResponse(
        begrunnelse = fritaksvurdering.begrunnelse,
        vurderingsTidspunkt = fritaksvurdering.opprettetTid ?: LocalDateTime.now(),
        harFritak = fritaksvurdering.harFritak,
        fraDato = fritaksvurdering.fraDato,
        vurdertAv = VurdertAvResponse(
            ident = fritaksvurdering.vurdertAv,
            dato =
                fritaksvurdering.opprettetTid?.toLocalDate()
                    ?: error("Fant ikke opprettet tidspunkt for fritaksvurdering"),
            ansattnavn = ansattNavnOgEnhet?.navn,
            enhetsnavn = ansattNavnOgEnhet?.enhet
        )
    )
}