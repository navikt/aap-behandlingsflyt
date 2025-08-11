package no.nav.aap.behandlingsflyt.behandling.underveis

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.behandling.ansattinfo.AnsattInfoService
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.MeldepliktRegel
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.MeldepliktStatus
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvResponse
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.MeldepliktRimeligGrunnRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.RimeligGrunnVurdering
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.behandlingsflyt.tilgang.kanSaksbehandle
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.tilgang.BehandlingPathParam
import no.nav.aap.tilgang.getGrunnlag
import java.time.LocalDateTime
import javax.sql.DataSource

fun NormalOpenAPIRoute.meldepliktRimeligGrunnGrunnlagApi(
    dataSource: DataSource,
    repositoryRegistry: RepositoryRegistry
) {
    route("/api/behandling/{referanse}/grunnlag/meldeplikt-rimelig-grunn") {
        getGrunnlag<BehandlingReferanse, MeldepliktRimeligGrunnGrunnlagResponse>(
            behandlingPathParam = BehandlingPathParam("referanse"),
            avklaringsbehovKode = Definisjon.OVERSTYR_IKKE_OPPFYLT_MELDEPLIKT.kode.toString(),
        ) { req ->
            val meldepliktGrunnlag =
                dataSource.transaction(readOnly = true) { connection ->
                    val repositoryProvider = repositoryRegistry.provider(connection)
                    val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                    val meldepliktRimeligGrunnRepository =
                        repositoryProvider.provide<MeldepliktRimeligGrunnRepository>()
                    val underveisRepository = repositoryProvider.provide<UnderveisRepository>()

                    val behandling: Behandling =
                        BehandlingReferanseService(behandlingRepository).behandling(req)

                    val nåTilstand = meldepliktRimeligGrunnRepository.hentHvisEksisterer(behandling.id)?.vurderinger

                    val vedtatteVerdier =
                        behandling.forrigeBehandlingId?.let { meldepliktRimeligGrunnRepository.hentHvisEksisterer(it) }?.vurderinger
                            ?: emptyList()
                    val historikk =
                        meldepliktRimeligGrunnRepository.hentAlleVurderinger(behandling.sakId, behandling.id)

                    // Grunnlaget vil være tomt til underveis har kjørt, 
                    // men siden det er frivillig overstyring der steget automatisk returnerer Fullført, er dette greit
                    val underveisGrunnlag = underveisRepository.hentHvisEksisterer(behandling.id)

                    MeldepliktRimeligGrunnGrunnlagResponse(
                        harTilgangTilÅSaksbehandle = kanSaksbehandle(),
                        perioderIkkeMeldt = underveisGrunnlag?.perioder
                            ?.filter { it.meldepliktStatus == MeldepliktStatus.IKKE_MELDT_SEG }
                            ?.map { it.meldePeriode }
                            ?: emptyList(),
                        perioderRimeligGrunn = underveisGrunnlag?.perioder
                            ?.filter { it.meldepliktStatus == MeldepliktStatus.RIMELIG_GRUNN }
                            ?.map { it.meldePeriode }
                            ?: emptyList(),
                        historikk =
                            historikk
                                .map { tilResponse(it) }
                                .sortedBy { it.vurderingsTidspunkt }
                                .toSet(),
                        gjeldendeVedtatteVurderinger =
                            vedtatteVerdier
                                .map { tilResponse(it) }
                                .sortedBy { it.fraDato },
                        vurderinger =
                            nåTilstand
                                ?.filterNot { vedtatteVerdier.contains(it) }
                                ?.map { tilResponse(it) }
                                ?.sortedBy { it.fraDato } ?: emptyList()
                    )
                }

            respond(meldepliktGrunnlag)
        }
    }
}

private fun tilResponse(rimeligGrunnVurdering: RimeligGrunnVurdering): MeldepliktRimeligGrunnVurderingResponse {
    val ansattNavnOgEnhet = AnsattInfoService(GatewayProvider).hentAnsattNavnOgEnhet(rimeligGrunnVurdering.vurdertAv)

    return MeldepliktRimeligGrunnVurderingResponse(
        begrunnelse = rimeligGrunnVurdering.begrunnelse,
        vurderingsTidspunkt = rimeligGrunnVurdering.opprettetTid ?: LocalDateTime.now(),
        harRimeligGrunn = rimeligGrunnVurdering.harRimeligGrunn,
        fraDato = rimeligGrunnVurdering.fraDato,
        vurdertAv = VurdertAvResponse(
            ident = rimeligGrunnVurdering.vurdertAv,
            dato =
                rimeligGrunnVurdering.opprettetTid?.toLocalDate()
                    ?: error("Fant ikke opprettet tidspunkt for rimeligGrunnVurdering"),
            ansattnavn = ansattNavnOgEnhet?.navn,
            enhetsnavn = ansattNavnOgEnhet?.enhet
        )
    )
}