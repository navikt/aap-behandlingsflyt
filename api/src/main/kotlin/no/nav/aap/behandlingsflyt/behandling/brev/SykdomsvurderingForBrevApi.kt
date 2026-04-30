package no.nav.aap.behandlingsflyt.behandling.brev

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvService
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdomsvurderingbrev.SykdomsvurderingForBrev
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdomsvurderingbrev.SykdomsvurderingForBrevRepository
import no.nav.aap.behandlingsflyt.kanLøseBehovSomSkalVæreLåstEtterKvalitetssikring
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.tilgang.kanSaksbehandle
import no.nav.aap.behandlingsflyt.tilgang.relevanteIdenterForBehandlingResolver
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
    route("/api/behandling/{referanse}/grunnlag/sykdomsvurdering-for-brev") {
        getGrunnlag<BehandlingReferanse, SykdomsvurderingForBrevDto>(
            relevanteIdenterResolver = relevanteIdenterForBehandlingResolver(repositoryRegistry, dataSource),
            behandlingPathParam = BehandlingPathParam("referanse"),
            påkrevdRolle = Definisjon.SKRIV_SYKDOMSVURDERING_BREV.løsesAv
        ) { behandlingReferanse ->
            val grunnlag = dataSource.transaction { connection ->
                val repositoryProvider = repositoryRegistry.provider(connection)
                val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                val sykdomsvurderingForBrevRepository = repositoryProvider.provide<SykdomsvurderingForBrevRepository>()
                val vurdertAvService = VurdertAvService(repositoryProvider, gatewayProvider)
                val behandling = behandlingRepository.hent(behandlingReferanse)

                val sykdomsvurderingForBrev = hentSykdomsvurderingForBrev(
                    behandlingReferanse,
                    behandlingRepository,
                    sykdomsvurderingForBrevRepository
                )
                val historiskeSykdomsvurderingerForBrev = hentHistoriskeSykdomsvurderingerForBrev(
                    behandlingReferanse,
                    behandlingRepository,
                    sykdomsvurderingForBrevRepository
                )

                SykdomsvurderingForBrevDto(
                    vurdering = sykdomsvurderingForBrev?.toDto(
                        vurdertAvService,
                        behandlingRepository.hent(behandlingReferanse)
                    ),
                    historiskeVurderinger = historiskeSykdomsvurderingerForBrev.map {
                        it.toDto(
                            vurdertAvService,
                            behandlingRepository.hent(behandlingReferanse)
                        )
                    },
                    kanSaksbehandle = kanSaksbehandle() && kanLøseBehovSomSkalVæreLåstEtterKvalitetssikring(Definisjon.SKRIV_SYKDOMSVURDERING_BREV.løsesISteg, behandling),
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
    sykdomsvurderingForBrevRepository: SykdomsvurderingForBrevRepository
): List<SykdomsvurderingForBrev> {
    val behandling = behandlingRepository.hent(behandlingReferanse)
    return sykdomsvurderingForBrevRepository.hent(behandling.sakId)
        .filter { it.behandlingId != behandling.id }
        .sortedByDescending { it.vurdertTidspunkt }
}

private fun SykdomsvurderingForBrev.toDto(
    vurdertAvService: VurdertAvService,
    behandling: Behandling
): SykdomsvurderingForBrevVurderingDto {
    return SykdomsvurderingForBrevVurderingDto(
        vurdering = vurdering,
        vurderingerMeta = vurdertAvService.byggVurderingerMeta(
            definisjon = Definisjon.SKRIV_SYKDOMSVURDERING_BREV,
            behandlingId = behandling.id,
            vurdertAv = vurdertAvService.medNavnOgEnhet(
                ident = vurdertAv,
                dato = vurdertTidspunkt.toLocalDate(),
            ),
        ),
    )
}