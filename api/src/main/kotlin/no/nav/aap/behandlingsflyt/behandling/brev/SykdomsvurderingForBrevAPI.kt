package no.nav.aap.behandlingsflyt.behandling.brev

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.behandling.ansattinfo.AnsattInfoService
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvResponse
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdomsvurderingbrev.SykdomsvurderingForBrev
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdomsvurderingbrev.SykdomsvurderingForBrevRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.tilgang.kanSaksbehandle
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.tilgang.BehandlingPathParam
import no.nav.aap.tilgang.getGrunnlag
import javax.sql.DataSource

fun NormalOpenAPIRoute.sykdomsvurderingForBrevApi(dataSource: DataSource, repositoryRegistry: RepositoryRegistry) {
    route("/api/behandling/{referanse}/grunnlag/sykdomsvurdering-for-brev") {
        getGrunnlag<BehandlingReferanse, SykdomsvurderingForBrevDto>(
            behandlingPathParam = BehandlingPathParam("referanse"),
            avklaringsbehovKode = Definisjon.SKRIV_SYKDOMSVURDERING_BREV.kode.toString()

        ) { behandlingReferanse ->
            val sykdomsvurderingForBrev = hentSykdomsvurderingForBrev(dataSource, behandlingReferanse, repositoryRegistry)
            respond(SykdomsvurderingForBrevDto(sykdomsvurderingForBrev?.toDto(), kanSaksbehandle()))
        }
    }
}

private fun hentSykdomsvurderingForBrev(
    dataSource: DataSource,
    behandlingReferanse: BehandlingReferanse,
    repositoryRegistry: RepositoryRegistry
): SykdomsvurderingForBrev? {
    return dataSource.transaction { connection ->
        val repositoryProvider = repositoryRegistry.provider(connection)
        val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
        val sykdomsvurderingForBrevRepository = repositoryProvider.provide<SykdomsvurderingForBrevRepository>()

        val behandling = behandlingRepository.hent(behandlingReferanse)
        sykdomsvurderingForBrevRepository.hent(behandling.id)
    }
}

private fun SykdomsvurderingForBrev.toDto(): SykdomsvurderingForBrevVurderingDto {
    val ansattNavnOgEnhet = AnsattInfoService().hentAnsattNavnOgEnhet(vurdertAv)
    return SykdomsvurderingForBrevVurderingDto(
        vurdering = vurdering,
        vurdertAv = VurdertAvResponse(
            ident = vurdertAv,
            dato = requireNotNull(vurdertTidspunkt?.toLocalDate()) { "Fant ikke vurdert tidspunkt for sykdomsvurdering for brev" },
            ansattnavn = ansattNavnOgEnhet?.navn,
            enhetsnavn = ansattNavnOgEnhet?.enhet
        )
    )
}