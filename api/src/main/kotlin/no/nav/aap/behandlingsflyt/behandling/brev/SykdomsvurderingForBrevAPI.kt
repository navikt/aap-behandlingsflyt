package no.nav.aap.behandlingsflyt.behandling.brev

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.behandling.ansattinfo.AnsattInfoService
import no.nav.aap.behandlingsflyt.behandling.avbrytrevurdering.AvbrytRevurderingService
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvResponse
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdomsvurderingbrev.SykdomsvurderingForBrev
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdomsvurderingbrev.SykdomsvurderingForBrevRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.tilgang.kanSaksbehandle
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.tilgang.BehandlingPathParam
import no.nav.aap.tilgang.getGrunnlag
import javax.sql.DataSource

fun NormalOpenAPIRoute.sykdomsvurderingForBrevApi(
    dataSource: DataSource,
    repositoryRegistry: RepositoryRegistry,
    gatewayProvider: GatewayProvider,
) {
    val ansattInfoService = AnsattInfoService(gatewayProvider)
    route("/api/behandling/{referanse}/grunnlag/sykdomsvurdering-for-brev") {
        getGrunnlag<BehandlingReferanse, SykdomsvurderingForBrevDto>(
            behandlingPathParam = BehandlingPathParam("referanse"),
            avklaringsbehovKode = Definisjon.SKRIV_SYKDOMSVURDERING_BREV.kode.toString()

        ) { behandlingReferanse ->
            val grunnlag = dataSource.transaction { connection ->
                val repositoryProvider = repositoryRegistry.provider(connection)
                val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                val sykdomsvurderingForBrevRepository = repositoryProvider.provide<SykdomsvurderingForBrevRepository>()
                val avbrytRevurderingService = AvbrytRevurderingService(repositoryProvider)

                val sykdomsvurderingForBrev = hentSykdomsvurderingForBrev(behandlingReferanse, behandlingRepository, sykdomsvurderingForBrevRepository)
                val historiskeSykdomsvurderingerForBrev = hentHistoriskeSykdomsvurderingerForBrev(behandlingReferanse, behandlingRepository, sykdomsvurderingForBrevRepository, avbrytRevurderingService)

                SykdomsvurderingForBrevDto(
                    vurdering = sykdomsvurderingForBrev?.toDto(ansattInfoService),
                    historiskeVurderinger = historiskeSykdomsvurderingerForBrev.map { it.toDto(ansattInfoService) },
                    kanSaksbehandle = kanSaksbehandle()
                )

            }
            respond(grunnlag)
        }
    }
}

private fun hentSykdomsvurderingForBrev(
    behandlingReferanse: BehandlingReferanse,
    behandlingRepository: BehandlingRepository,
    sykdomsvurderingForBrevRepository: SykdomsvurderingForBrevRepository
): SykdomsvurderingForBrev? {
    val behandling = behandlingRepository.hent(behandlingReferanse)
    return sykdomsvurderingForBrevRepository.hent(behandling.id)
}

private fun hentHistoriskeSykdomsvurderingerForBrev(
    behandlingReferanse: BehandlingReferanse,
    behandlingRepository: BehandlingRepository,
    sykdomsvurderingForBrevRepository: SykdomsvurderingForBrevRepository,
    avbrytRevurderingService: AvbrytRevurderingService
): List<SykdomsvurderingForBrev> {
    val behandling = behandlingRepository.hent(behandlingReferanse)
    val behandlingerIderMedAvbrutteRevurdering =
        avbrytRevurderingService.hentBehandlingerMedAvbruttRevurderingForSak(behandling.sakId)
            .map { it.id }
    return sykdomsvurderingForBrevRepository.hent(behandling.sakId)
        .filter { it.behandlingId != behandling.id }
        .filter { it.behandlingId !in behandlingerIderMedAvbrutteRevurdering }
        .sortedByDescending { it.vurdertTidspunkt }
}

private fun SykdomsvurderingForBrev.toDto(ansattInfoService: AnsattInfoService): SykdomsvurderingForBrevVurderingDto {
    val ansattNavnOgEnhet = ansattInfoService.hentAnsattNavnOgEnhet(vurdertAv)
    return SykdomsvurderingForBrevVurderingDto(
        vurdering = vurdering,
        vurdertAv = VurdertAvResponse(
            ident = vurdertAv,
            dato = vurdertTidspunkt.toLocalDate(),
            ansattnavn = ansattNavnOgEnhet?.navn,
            enhetsnavn = ansattNavnOgEnhet?.enhet
        )
    )
}