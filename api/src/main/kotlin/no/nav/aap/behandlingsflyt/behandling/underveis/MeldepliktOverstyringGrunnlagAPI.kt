package no.nav.aap.behandlingsflyt.behandling.underveis

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.behandling.ansattinfo.AnsattInfoService
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.MeldepliktStatus
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvResponse
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.OverstyringMeldepliktRepository
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
import javax.sql.DataSource

fun NormalOpenAPIRoute.meldepliktOverstyringGrunnlagApi(
    dataSource: DataSource,
    repositoryRegistry: RepositoryRegistry,
    gatewayProvider: GatewayProvider,
) {
    val ansattInfoService = AnsattInfoService(gatewayProvider)

    route("/api/behandling/{referanse}/grunnlag/meldeplikt-overstyring") {
        getGrunnlag<BehandlingReferanse, MeldepliktOverstyringGrunnlagResponse>(
            behandlingPathParam = BehandlingPathParam("referanse"),
            avklaringsbehovKode = Definisjon.OVERSTYR_IKKE_OPPFYLT_MELDEPLIKT.kode.toString(),
        ) { req ->
            val meldepliktGrunnlag =
                dataSource.transaction(readOnly = true) { connection ->
                    val repositoryProvider = repositoryRegistry.provider(connection)
                    val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                    val overstyringMeldepliktRepository =
                        repositoryProvider.provide<OverstyringMeldepliktRepository>()
                    val underveisRepository = repositoryProvider.provide<UnderveisRepository>()

                    val behandling: Behandling =
                        BehandlingReferanseService(behandlingRepository).behandling(req)

                    val gjeldendeVurdertePerioder = overstyringMeldepliktRepository
                        .hentHvisEksisterer(behandling.id)
                        ?.tilTidslinje()
                        ?.segmenter()
                        ?.map { segment ->
                            MeldepliktOverstyringVurderingResponse(
                                begrunnelse = segment.verdi.begrunnelse,
                                vurderingsTidspunkt = segment.verdi.opprettetTid,
                                vurdertIBehandling = segment.verdi.vurdertIBehandling,
                                meldepliktOverstyringStatus = segment.verdi.meldepliktOverstyringStatus,
                                fraDato = segment.periode.fom,
                                tilDato = segment.periode.tom,
                                vurdertAv = VurdertAvResponse.fraIdent(segment.verdi.vurdertAv, segment.verdi.opprettetTid.toLocalDate(), ansattInfoService)
                            )
                        } ?: emptyList()

                    // Grunnlaget vil være tomt til underveis har kjørt, 
                    // men siden det er frivillig overstyring der steget automatisk returnerer Fullført, er dette greit
                    val underveisGrunnlag = underveisRepository.hentHvisEksisterer(behandling.id)

                    MeldepliktOverstyringGrunnlagResponse(
                        harTilgangTilÅSaksbehandle = kanSaksbehandle(),
                        perioderIkkeMeldt = underveisGrunnlag?.perioder
                            ?.filter { it.meldepliktStatus == MeldepliktStatus.IKKE_MELDT_SEG }
                            ?.map { it.meldePeriode }
                            ?: emptyList(),
                        gjeldendeVedtatteOversyringsvurderinger = gjeldendeVurdertePerioder.filter { it.vurdertIBehandling != behandling.referanse },
                        overstyringsvurderinger =  gjeldendeVurdertePerioder.filter { it.vurdertIBehandling == behandling.referanse }
                    )
                }

            respond(meldepliktGrunnlag)
        }
    }
}
